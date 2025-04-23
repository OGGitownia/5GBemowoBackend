import zipfile
import os
import shutil
import xml.etree.ElementTree as ET


def extract_images_and_replace_drawings(docx_path: str, output_dir: str):
    temp_dir = "_unpacked_docx"
    photos_dir = os.path.join(output_dir, "photos")
    os.makedirs(photos_dir, exist_ok=True)

    # 1. Rozpakuj plik docx jako zip
    with zipfile.ZipFile(docx_path, 'r') as zip_ref:
        zip_ref.extractall(temp_dir)

    # 2. Przenieś obrazy z word/media do katalogu photos
    media_dir = os.path.join(temp_dir, "word", "media")
    if os.path.exists(media_dir):
        for i, file in enumerate(os.listdir(media_dir), start=1):
            ext = os.path.splitext(file)[1].lower()
            new_name = f"photo_{i}{ext}"
            shutil.copy(os.path.join(media_dir, file), os.path.join(photos_dir, new_name))

    # 3. Podmień rysunki w document.xml na tekst
    document_xml_path = os.path.join(temp_dir, "word", "document.xml")
    tree = ET.parse(document_xml_path)
    root = tree.getroot()
    ns = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}

    # Znajdź wszystkie <w:drawing> i zamień na <w:t>z tekstem</w:t>
    drawing_count = 0
    for drawing in root.findall(".//w:drawing", ns):
        drawing_count += 1
        parent = drawing.find("..")  # nie działa w xml.etree, więc musimy obejść to inaczej
        # Tworzymy nowy w:r z tekstem
        new_run = ET.Element("{http://schemas.openxmlformats.org/wordprocessingml/2006/main}r")
        new_text = ET.SubElement(new_run, "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}t")
        new_text.text = f"{{zdjęcie zapisane jako: photo_{drawing_count}.<format>}}"
        # Zamiana elementu
        drawing.clear()
        drawing.append(new_text)

    # Zapisz zmodyfikowany dokument
    tree.write(document_xml_path, encoding="utf-8", xml_declaration=True)

    # 4. Spakuj ponownie do nowego .docx
    output_docx_path = os.path.join(output_dir, "modified_with_image_refs.docx")
    with zipfile.ZipFile(output_docx_path, 'w', zipfile.ZIP_DEFLATED) as docx:
        for foldername, subfolders, filenames in os.walk(temp_dir):
            for filename in filenames:
                file_path = os.path.join(foldername, filename)
                arcname = os.path.relpath(file_path, temp_dir)
                docx.write(file_path, arcname)

    # 5. Posprzątaj tymczasowe pliki
    shutil.rmtree(temp_dir)

    print(f"Wszystkie zdjęcia zapisane w: {photos_dir}")
    print(f"Zmodyfikowany dokument zapisany jako: {output_docx_path}")


# Przykład użycia:
# extract_images_and_replace_drawings("/ścieżka/do/pliku.docx", "ścieżka/do/output")