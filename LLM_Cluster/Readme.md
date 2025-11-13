
# LLM Cluster

Het LLM Cluster is bedoelt voor het managen en het organiseren van de LLMs. 

```
|_ processor
    Python api voor de API wrapper om het embedden en de LLM heen. 
```

## Setup Instructies

### 1. Repository Klonen

```bash
git clone https://github.com/ScriptSparrow/Spoordok_ICTDT.git
cd Spoordok_ICTDT/LLM_Cluster/processor
```

### 2. Python Virtual Environment Aanmaken

Maak een virtuele omgeving aan om dependencies geïsoleerd te installeren:

```bash
cd Spoordok_ICTDT/LLM_Cluster/processor
python -m venv venv
```

### 3. Virtual Environment Activeren

**Windows (PowerShell):**
```powershell
.\venv\Scripts\Activate.ps1
```

**Windows (CMD):**
```cmd
.\venv\Scripts\activate.bat
```

**Linux/Mac:**
```bash
source venv/bin/activate
```

### 4. Dependencies Installeren

Installeer alle benodigde packages via requirements.txt:

```bash
pip install -r requirements.txt
```

### 5. VS Code Configureren

Om ervoor te zorgen dat VS Code de juiste Python interpreter gebruikt:

1. Open het Command Palette (`Ctrl+Shift+P` of `Cmd+Shift+P` op Mac)
2. Typ: `Python: Select Interpreter`
3. Selecteer de interpreter met het pad: `.\venv\Scripts\python.exe` (Windows) of `./venv/bin/python` (Linux/Mac)

**Alternatief: Automatische configuratie via settings.json**

Maak een `.vscode` map aan in de root van het project en voeg een `settings.json` bestand toe:

```json
{
    "python.defaultInterpreterPath": "${workspaceFolder}/LLM_Cluster/processor/venv/Scripts/python.exe"
}
```

Voor Linux/Mac gebruik:
```json
{
    "python.defaultInterpreterPath": "${workspaceFolder}/LLM_Cluster/processor/venv/bin/python"
}
```

### 6. FastAPI Applicatie Starten



## Dependencies beheren

Dit project gebruikt `pip-tools` om dependencies te beheren:

```bash

#Navigeer naar de juiste directory
cd Spoordok_ICTDT/LLM_Cluster/processor

# Installeer pip-tools
pip install pip-tools

# Update requirements.txt van requirements.in
pip-compile --constraint constraints.txt requirements.in

# Update naar laatste versies
pip-compile --constraint constraints.txt --upgrade requirements.in

# Installeer dependencies
pip install -r requirements.txt
```