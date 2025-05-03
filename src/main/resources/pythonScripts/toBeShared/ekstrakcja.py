import zipfile
import os
import shutil
from lxml import etree as ET


def extract_images_and_replace_drawings(docx_path: str, output_dir: str):
    temp_dir = "_unpacked_docx"
    photos_dir = os.path.join(output_dir, "photos")
    os.makedirs(photos_dir, exist_ok=True)

    # 1. Rozpakuj plik docx jako zip
    with zipfile.ZipFile(docx_path, 'r') as zip_ref:
        zip_ref.extractall(temp_dir)

    # 2. Przenieś obrazy z word/media do katalogu photos
    media_dir = os.path.join(temp_dir, "word", "media")
    image_mapping = {}
    if os.path.exists(media_dir):
        for i, file in enumerate(sorted(os.listdir(media_dir)), start=1):
            ext = os.path.splitext(file)[1].lower()
            new_name = f"photo_{i}{ext}"
            shutil.copy(os.path.join(media_dir, file), os.path.join(photos_dir, new_name))
            image_mapping[file] = new_name

    # 3. Podmień <w:drawing> i <v:imagedata> na tekstowe reprezentacje
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

    # Pomocnicza funkcja do znajdowania rodzica danego typu
    def find_ancestor(tag_name, elem):
        while elem is not None:
            elem = elem.getparent()
            if elem is not None and elem.tag == tag_name:
                return elem
        return None

    # 3a. Obsługa <w:drawing>
    for drawing in root.xpath(".//w:drawing", namespaces=ns):
        run = drawing.getparent()
        if run.tag != f"{{{ns['w']}}}r":
            continue

        drawing_count += 1
        new_run = ET.Element(f"{{{ns['w']}}}r")
        new_text = ET.SubElement(new_run, f"{{{ns['w']}}}t")
        new_text.text = f"{{zdjęcie zapisane jako: photo_{drawing_count}.<format>}}"

        run_parent = run.getparent()
        run_index = run_parent.index(run)
        run_parent.remove(run)
        run_parent.insert(run_index, new_run)

    # 3b. Obsługa <v:imagedata>
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

            drawing_count += 1
            new_filename = image_mapping.get(target, f"photo_{drawing_count}.<format>")

            new_run = ET.Element(f"{{{ns['w']}}}r")
            new_text = ET.SubElement(new_run, f"{{{ns['w']}}}t")
            new_text.text = f"{{zdjęcie zapisane jako: {new_filename}}}"

            run_parent = parent_r.getparent()
            run_index = run_parent.index(parent_r)
            run_parent.remove(parent_r)
            run_parent.insert(run_index, new_run)

    # 4. Zapisz zmodyfikowany dokument.xml
    tree.write(document_xml_path, encoding="utf-8", xml_declaration=True, pretty_print=True)

    # 5. Spakuj ponownie do nowego .docx
    output_docx_path = os.path.join(output_dir, "modified_with_image_refs.docx")
    with zipfile.ZipFile(output_docx_path, 'w', zipfile.ZIP_DEFLATED) as docx:
        for foldername, subfolders, filenames in os.walk(temp_dir):
            for filename in filenames:
                file_path = os.path.join(foldername, filename)
                arcname = os.path.relpath(file_path, temp_dir)
                docx.write(file_path, arcname)

    # 6. Posprzątaj tymczasowe pliki
    shutil.rmtree(temp_dir)

    print(f"Wszystkie zdjęcia zapisane w: {photos_dir}")
    print(f"Zmodyfikowany dokument zapisany jako: {output_docx_path}")

extract_images_and_replace_drawings("36331-g30.docx", "resor3")