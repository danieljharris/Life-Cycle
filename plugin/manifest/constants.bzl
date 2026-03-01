ARTIFACT_NAME = "LifeCycle"
VERSION = "1.1.2"
GROUP = "DrDan"

# https://maven.hytale.com/pre-release/com/hypixel/hytale/Server/maven-metadata.xml
# [HytaleServer] Booting up HytaleServer - Version: 2026.02.11-255364b8e, Revision: 255364b8e3ae86321347e08735c113cf6f10de06
SERVER_VERSION = "2026.02.18-f3b8fff95"

CONFIG = {
    "Version": VERSION,
    "Name": ARTIFACT_NAME,
    "Group": GROUP,
    "ServerVersion": SERVER_VERSION,
    "Main": GROUP + ".LifeCycle.LifeCycleMain",
    "Description": "Adds animal growth. Allows baby animals to naturally grow into adults over time!",
    "IncludesAssetPack": True,
    "Authors": [
        {
            "Name": "Dr Daniel Harris",
            "Email": "danieljharris26@yahoo.co.uk",
            "Url": "https://github.com/danieljharris",
        },
    ],
}
