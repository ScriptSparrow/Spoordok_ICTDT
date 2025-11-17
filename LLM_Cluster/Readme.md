
# LLM Cluster

Het LLM Cluster is bedoelt voor het managen en het organiseren van de LLMs. 

```
|_ processor
    Python api voor de API wrapper om het embedden en de LLM heen. 
```

## Ollama



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



# TODO: Multi-turn Reasoning Enhancement

Goal: Enable iterative (dialog) analysis over previously retrieved captions (and later images).

Tasks:
1. Conversation State
   - Design SessionMemory (stores last K user queries + retrieved point_ids + model responses).
2. Context Expansion
   - Implement follow-up query fusion: new_embedding = w1*current + w2*mean(previous successful caption embeddings).
3. Re-Ranking
   - Add conversational relevance scoring: boost points mentioned in prior turns; decay stale ones.
4. Clarification Handling
   - Detect underspecified queries; auto-generate clarifying questions using VLM.
5. Intent Shift Detection
   - Simple classifier (embedding distance or keyword diff) to decide when to reset context.
6. Deferred Image Access
   - Placeholder to fetch original image bytes only if user drills down.
7. Evaluation
   - Metrics: Conversational Recall@K over scripted multi-turn scenarios.
8. Future (when image embeddings added)
   - Cross-modal co-reference: map pronouns (“this”, “that scene”) to prior image vectors.

Open Questions:
- Max turns to keep? (default 5)
- Store chain-of-thought or just final answers?
- Need per-context throttle to avoid vector spam?

Dependencies:
- Add SessionMemory class (in a new session.py).
- Extend QDrantConnector to batch fetch by point_id list.