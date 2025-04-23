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
    drawing_map = {}
    if os.path.exists(media_dir):
        for i, file in enumerate(sorted(os.listdir(media_dir)), start=1):
            ext = os.path.splitext(file)[1].lower()
            new_name = f"photo_{i}{ext}"
            shutil.copy(os.path.join(media_dir, file), os.path.join(photos_dir, new_name))
            drawing_map[file] = new_name

    # 3. Podmień rysunki w document.xml na tekst
    document_xml_path = os.path.join(temp_dir, "word", "document.xml")
    parser = ET.XMLParser(remove_blank_text=True)
    tree = ET.parse(document_xml_path, parser)
    root = tree.getroot()

    nsmap = {
        'w': "http://schemas.openxmlformats.org/wordprocessingml/2006/main",
        'a': "http://schemas.openxmlformats.org/drawingml/2006/main",
        'r': "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
        'wp': "http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
    }

    drawing_elements = root.xpath(".//w:drawing", namespaces=nsmap)
    for idx, drawing in enumerate(drawing_elements, start=1):
        parent = drawing.getparent()
        if parent is not None:
            new_run = ET.Element("{http://schemas.openxmlformats.org/wordprocessingml/2006/main}r")
            new_text = ET.Element("{http://schemas.openxmlformats.org/wordprocessingml/2006/main}t")
            new_text.text = f"{{zdjęcie zapisane jako: photo_{idx}.<format>}}"
            new_run.append(new_text)
            parent.replace(drawing, new_run)

    # 4. Zapisz zmodyfikowany document.xml
    tree.write(document_xml_path, pretty_print=True, xml_declaration=True, encoding="UTF-8")

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


# Przykład użycia:
# extract_images_and_replace_drawings("/ścieżka/do/pliku.docx", "ścieżka/do/output")