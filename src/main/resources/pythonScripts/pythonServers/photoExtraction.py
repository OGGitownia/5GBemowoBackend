from flask import Flask, request, jsonify
import threading
import signal
import sys
import requests
import zipfile
import os
import shutil
from lxml import etree as ET

sys.stdout.reconfigure(encoding='utf-8')

app = Flask(__name__)

server_name = sys.argv[1] if len(sys.argv) > 1 else "serverTemplete"
port = int(sys.argv[2]) if len(sys.argv) > 2 else 5003

SPRING_BOOT_NOTIFY = f"http://localhost:8080/{server_name}/server-ready"

@app.route(f"/{server_name}/process", methods=["POST"])
def process_embedding_request():
    print("process request", flush=True)
    try:
        data = request.get_json()
        print(f"Received JSON: {data}", flush=True)

        input_path = data.get("input")
        output_docx_path = data.get("outputDocx")
        output_dir_path = data.get("outputDir")

        print(f"Input path: {input_path}", flush=True)
        print(f"Output DOCX path: {output_docx_path}", flush=True)
        print(f"Output directory: {output_dir_path}", flush=True)

        extract_images_and_replace_drawings(input_path, output_docx_path, output_dir_path)

        return jsonify({"message": "Processing ended"}), 200

    except Exception as e:
        import traceback
        print(traceback.format_exc(), flush=True)
        return jsonify({"error": str(e)}), 500

def extract_images_and_replace_drawings(docx_path: str, output_docx_path2: str, output_dir: str):
    temp_dir = "_unpacked_docx"
    photos_dir = os.path.join(output_dir, "photos")
    os.makedirs(photos_dir, exist_ok=True)

    with zipfile.ZipFile(docx_path, 'r') as zip_ref:
        zip_ref.extractall(temp_dir)

    media_dir = os.path.join(temp_dir, "word", "media")
    image_mapping = {}
    if os.path.exists(media_dir):
        for i, file in enumerate(sorted(os.listdir(media_dir)), start=1):
            ext = os.path.splitext(file)[1].lower()
            new_name = f"photo_{i}{ext}"
            shutil.copy(os.path.join(media_dir, file), os.path.join(photos_dir, new_name))
            image_mapping[file] = new_name

    document_xml_path = os.path.join(temp_dir, "word", "document.xml")
    rels_path = os.path.join(temp_dir, "word", "_rels", "document.xml.rels")

    parser = ET.XMLParser(remove_blank_text=True)
    tree = ET.parse(document_xml_path, parser)
    root = tree.getroot()

    ns = {
        'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main',
        'r': 'http://schemas.openxmlformats.org/officeDocument/2006/relationships',
        'v': 'urn:schemas-microsoft-com:vml',
        'o': 'urn:schemas-microsoft-com:office:office'
    }
    ET.register_namespace('w', ns['w'])

    drawing_count = 0

    def find_ancestor(tag_name, elem):
        while elem is not None:
            elem = elem.getparent()
            if elem is not None and elem.tag == tag_name:
                return elem
        return None

    def extract_and_remove_caption(current_p):
        parent = current_p.getparent()
        index = parent.index(current_p)
        if index + 1 < len(parent):
            next_p = parent[index + 1]
            texts = next_p.xpath(".//w:t", namespaces=ns)
            caption_texts = [t.text.strip() for t in texts if t.text]
            full_caption = ' '.join(caption_texts)
            if full_caption.lower().startswith("figure"):
                parent.remove(next_p)
                return full_caption
        return None

    # Obsługa <w:drawing>
    for drawing in root.xpath(".//w:drawing", namespaces=ns):
        run = drawing.getparent()
        if run.tag != f"{{{ns['w']}}}r":
            continue

        paragraph = find_ancestor(f"{{{ns['w']}}}p", run)
        caption = extract_and_remove_caption(paragraph)

        drawing_count += 1
        ext = ".jpg"
        filename = f"photo_{drawing_count}{ext}"

        new_run = ET.Element(f"{{{ns['w']}}}r")
        new_text = ET.SubElement(new_run, f"{{{ns['w']}}}t")
        if caption:
            new_text.text = f"'{filename} : {caption}'"
        else:
            new_text.text = f"'{filename}'"

        run_parent = run.getparent()
        run_index = run_parent.index(run)
        run_parent.remove(run)
        run_parent.insert(run_index, new_run)

    # Obsługa <v:imagedata>
    if os.path.exists(rels_path):
        rels_tree = ET.parse(rels_path)
        rels_root = rels_tree.getroot()

        for imagedata in root.xpath(".//v:imagedata", namespaces=ns):
            parent_r = find_ancestor(f"{{{ns['w']}}}r", imagedata)
            if parent_r is None:
                continue

            rid = imagedata.get(f"{{{ns['r']}}}id")
            target = None
            for rel in rels_root:
                if rel.get("Id") == rid:
                    target = os.path.basename(rel.get("Target"))
                    break

            paragraph = find_ancestor(f"{{{ns['w']}}}p", parent_r)
            caption = extract_and_remove_caption(paragraph)

            drawing_count += 1
            new_filename = image_mapping.get(target, f"photo_{drawing_count}.jpg")

            new_run = ET.Element(f"{{{ns['w']}}}r")
            new_text = ET.SubElement(new_run, f"{{{ns['w']}}}t")
            if caption:
                new_text.text = f"'{new_filename} : {caption}'"
            else:
                new_text.text = f"'{new_filename}'"

            run_parent = parent_r.getparent()
            run_index = run_parent.index(parent_r)
            run_parent.remove(parent_r)
            run_parent.insert(run_index, new_run)

    tree.write(document_xml_path, encoding="utf-8", xml_declaration=True, pretty_print=True)

    output_docx_path = output_docx_path2
    with zipfile.ZipFile(output_docx_path, 'w', zipfile.ZIP_DEFLATED) as docx:
        for foldername, subfolders, filenames in os.walk(temp_dir):
            for filename in filenames:
                file_path = os.path.join(foldername, filename)
                arcname = os.path.relpath(file_path, temp_dir)
                docx.write(file_path, arcname)

    shutil.rmtree(temp_dir)

    print(f"Wszystkie zdjęcia zapisane w: {photos_dir}")
    print(f"Zmodyfikowany dokument zapisany jako: {output_docx_path}")

@app.route(f"/{server_name}/shutdown", methods=["POST"])
def shutdown():
    def shutdown_server():
        os.kill(os.getpid(), signal.SIGINT)
    response = jsonify({"message": "Shutting down server"})
    threading.Thread(target=shutdown_server).start()
    return response, 200

@app.route("/status", methods=["GET"])
def status():
    return jsonify({"status": "running"}), 200

def notify_spring_boot():
    print("Notifying Spring Boot that server is ready", flush=True)
    print(f"SPRING_BOOT_NOTIFY={SPRING_BOOT_NOTIFY}", flush=True)
    try:
        response = requests.post(SPRING_BOOT_NOTIFY, json={"port": port})
        print(f"Spring Boot responded: {response.status_code} {response.text}", flush=True)
    except Exception as e:
        print(f"Notification error: {e}", flush=True)

def run_server():
    notify_spring_boot()
    print(f"Server running on port {port}", flush=True)
    app.run(host="0.0.0.0", port=port)

if __name__ == "__main__":
    run_server()
