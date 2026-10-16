import urllib.request
import os
import zipfile
import shutil

JDK_DIR = os.path.expanduser(r"~\.jdks\microsoft-jdk-25")
os.makedirs(os.path.dirname(JDK_DIR), exist_ok=True)

if os.path.exists(os.path.join(JDK_DIR, "bin", "java.exe")):
    print("JDK 25 already installed at", JDK_DIR)
    exit(0)

ZIP_PATH = os.path.expanduser(r"~\.jdks\jdk25.zip")
URL = "https://aka.ms/download-jdk/microsoft-jdk-25-windows-x64.zip"

print(f"Downloading JDK 25 from {URL} to {ZIP_PATH}...")
req = urllib.request.Request(URL, headers={"User-Agent": "Mozilla/5.0"})
with urllib.request.urlopen(req) as resp, open(ZIP_PATH, "wb") as out:
    shutil.copyfileobj(resp, out)

print(f"Extracting {ZIP_PATH}...")
EXTRACT_TMP = os.path.expanduser(r"~\.jdks\tmp_extract")
os.makedirs(EXTRACT_TMP, exist_ok=True)
with zipfile.ZipFile(ZIP_PATH, "r") as z:
    z.extractall(EXTRACT_TMP)

# Find root folder containing bin
root_jdk = None
for root, dirs, files in os.walk(EXTRACT_TMP):
    if "bin" in dirs and os.path.exists(os.path.join(root, "bin", "java.exe")):
        root_jdk = root
        break

if root_jdk:
    if os.path.exists(JDK_DIR):
        shutil.rmtree(JDK_DIR)
    shutil.move(root_jdk, JDK_DIR)
    print("JDK 25 successfully extracted to", JDK_DIR)
else:
    print("Error: Could not locate bin/java.exe in extracted archive")

shutil.rmtree(EXTRACT_TMP, ignore_errors=True)
if os.path.exists(ZIP_PATH):
    os.remove(ZIP_PATH)
print("JDK 25 setup finished!")
