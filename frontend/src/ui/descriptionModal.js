import { getExampleDescription } from '../data/exampleDescriptions.js';

/**
 * Toont een modal dialoogvenster voor het invoeren van polygon naam en beschrijving.
 * @param {Object} feature - De aangemaakte feature
 * @param {string} buildingTypeName - De naam van het gebouwtype
 * @param {string} defaultName - De standaard gegenereerde naam (moet gewijzigd worden)
 * @param {Function} onSave - Callback wanneer gebruiker opslaat (ontvangt naam en beschrijving)
 * @param {Function} onCancel - Callback wanneer gebruiker annuleert
 */
export function showDescriptionModal(feature, buildingTypeName, defaultName, onSave, onCancel) {
    // Modal overlay aanmaken
    const overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.id = 'description-modal-overlay';

    const modal = document.createElement('div');
    modal.className = 'modal';
    modal.innerHTML = `
        <div class="modal-header">
            <h3>Polygon gegevens invoeren</h3>
        </div>
        <div class="modal-body">
            <label for="name-input">Naam (verplicht wijzigen):</label>
            <input 
                type="text" 
                id="name-input" 
                maxlength="255" 
                value="${defaultName}"
                placeholder="Voer een unieke naam in..."
            />
            <div class="field-hint error" id="name-hint">
                Wijzig de standaardnaam naar een beschrijvende naam.
            </div>
            
            <label for="description-input">Omschrijving (verplicht, max 450 karakters):</label>
            <textarea 
                id="description-input" 
                maxlength="450" 
                rows="5" 
                placeholder="Voer een beschrijving in..."
            ></textarea>
            <div class="char-count">
                <span id="char-counter">0</span>/450
            </div>
            <div class="field-hint error" id="description-hint">
                Beschrijving is verplicht.
            </div>
            <button type="button" id="btn-example-description" class="btn-secondary">
                Beschrijving voorbeeld
            </button>
        </div>
        <div class="modal-footer">
            <button type="button" id="btn-cancel" class="btn-cancel">Annuleren</button>
            <button type="button" id="btn-save" class="btn-primary">Opslaan</button>
        </div>
    `;

    overlay.appendChild(modal);
    document.body.appendChild(overlay);

    // Elementen ophalen
    const nameInput = modal.querySelector('#name-input');
    const nameHint = modal.querySelector('#name-hint');
    const textarea = modal.querySelector('#description-input');
    const charCounter = modal.querySelector('#char-counter');
    const descriptionHint = modal.querySelector('#description-hint');
    const btnExample = modal.querySelector('#btn-example-description');
    const btnSave = modal.querySelector('#btn-save');
    const btnCancel = modal.querySelector('#btn-cancel');

    // Karakter teller bijwerken en beschrijving validatie
    textarea.addEventListener('input', () => {
        charCounter.textContent = textarea.value.length;
        validateDescription();
    });

    // Naam validatie: controleer of het afwijkt van de standaard
    nameInput.addEventListener('input', () => {
        validateName();
    });

    // Validatie functies
    function validateName() {
        if (nameInput.value === defaultName || nameInput.value.trim() === '') {
            nameHint.classList.add('error');
            nameHint.textContent = 'De naam moet gewijzigd worden van de standaardwaarde!';
            return false;
        } else {
            nameHint.classList.remove('error');
            nameHint.textContent = 'Naam is correct ingevuld.';
            return true;
        }
    }

    function validateDescription() {
        if (textarea.value.trim() === '') {
            descriptionHint.classList.add('error');
            descriptionHint.textContent = 'Beschrijving is verplicht.';
            return false;
        } else {
            descriptionHint.classList.remove('error');
            descriptionHint.textContent = 'Beschrijving is correct ingevuld.';
            return true;
        }
    }

    // "Beschrijving voorbeeld" knop - haalt voorbeeld op basis van gebouwType
    btnExample.addEventListener('click', () => {
        const exampleText = getExampleDescription(buildingTypeName);
        textarea.value = exampleText;
        charCounter.textContent = exampleText.length;
        validateDescription();
    });

    // Opslaan knop
    btnSave.addEventListener('click', () => {
        const name = nameInput.value.trim();
        const description = textarea.value.trim();
        
        // Validatie uitvoeren
        const isNameValid = validateName();
        const isDescriptionValid = validateDescription();
        
        // Validatie: naam mag niet leeg zijn
        if (!name) {
            alert('Voer een naam in voor de polygon.');
            nameInput.focus();
            return;
        }
        
        // Validatie: naam moet gewijzigd zijn van de standaard
        if (!isNameValid) {
            alert('Wijzig de standaardnaam naar een unieke naam.');
            nameInput.focus();
            nameInput.select();
            return;
        }
        
        // Validatie: beschrijving mag niet leeg zijn
        if (!isDescriptionValid) {
            alert('Voer een beschrijving in.');
            textarea.focus();
            return;
        }
        
        closeModal();
        onSave(name, description);
    });

    // Annuleren knop
    btnCancel.addEventListener('click', () => {
        closeModal();
        if (onCancel) onCancel();
    });

    // Modal sluiten hulpfunctie
    function closeModal() {
        overlay.remove();
    }

    // Focus op naam input zetten
    nameInput.focus();
    nameInput.select();
}

/**
 * Toont een modal dialoogvenster voor het bewerken van een bestaande polygon.
 * @param {Object} feature - De te bewerken feature
 * @param {string} buildingTypeName - De naam van het gebouwtype
 * @param {string} currentName - De huidige naam van de polygon
 * @param {string} currentDescription - De huidige omschrijving van de polygon
 * @param {Function} onSave - Callback wanneer gebruiker opslaat (ontvangt naam en beschrijving)
 * @param {Function} onCancel - Callback wanneer gebruiker annuleert
 */
export function showEditModal(feature, buildingTypeName, currentName, currentDescription, onSave, onCancel) {
    // Modal overlay aanmaken
    const overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.id = 'edit-modal-overlay';

    const modal = document.createElement('div');
    modal.className = 'modal';
    modal.innerHTML = `
        <div class="modal-header">
            <h3>Polygon bewerken</h3>
        </div>
        <div class="modal-body">
            <label for="edit-name-input">Naam:</label>
            <input 
                type="text" 
                id="edit-name-input" 
                maxlength="255" 
                value="${currentName}"
                placeholder="Voer een naam in..."
            />
            <div class="field-hint" id="edit-name-hint">
                Geef de polygon een beschrijvende naam.
            </div>
            
            <label for="edit-description-input">Omschrijving (max 450 karakters):</label>
            <textarea 
                id="edit-description-input" 
                maxlength="450" 
                rows="5" 
                placeholder="Voer een beschrijving in..."
            >${currentDescription}</textarea>
            <div class="char-count">
                <span id="edit-char-counter">${currentDescription.length}</span>/450
            </div>
            <div class="field-hint" id="edit-description-hint">
                Beschrijving is verplicht.
            </div>
            <button type="button" id="btn-edit-example-description" class="btn-secondary">
                Beschrijving voorbeeld
            </button>
        </div>
        <div class="modal-footer">
            <button type="button" id="btn-edit-cancel" class="btn-cancel">Annuleren</button>
            <button type="button" id="btn-edit-save" class="btn-primary">Opslaan</button>
        </div>
    `;

    overlay.appendChild(modal);
    document.body.appendChild(overlay);

    // Elementen ophalen
    const nameInput = modal.querySelector('#edit-name-input');
    const nameHint = modal.querySelector('#edit-name-hint');
    const textarea = modal.querySelector('#edit-description-input');
    const charCounter = modal.querySelector('#edit-char-counter');
    const descriptionHint = modal.querySelector('#edit-description-hint');
    const btnExample = modal.querySelector('#btn-edit-example-description');
    const btnSave = modal.querySelector('#btn-edit-save');
    const btnCancel = modal.querySelector('#btn-edit-cancel');

    // Initiële validatie status
    validateName();
    validateDescription();

    // Karakter teller bijwerken en beschrijving validatie
    textarea.addEventListener('input', () => {
        charCounter.textContent = textarea.value.length;
        validateDescription();
    });

    // Naam validatie
    nameInput.addEventListener('input', () => {
        validateName();
    });

    // Validatie functies
    function validateName() {
        if (nameInput.value.trim() === '') {
            nameHint.classList.add('error');
            nameHint.textContent = 'Naam is verplicht.';
            return false;
        } else {
            nameHint.classList.remove('error');
            nameHint.textContent = 'Naam is correct ingevuld.';
            return true;
        }
    }

    function validateDescription() {
        if (textarea.value.trim() === '') {
            descriptionHint.classList.add('error');
            descriptionHint.textContent = 'Beschrijving is verplicht.';
            return false;
        } else {
            descriptionHint.classList.remove('error');
            descriptionHint.textContent = 'Beschrijving is correct ingevuld.';
            return true;
        }
    }

    // "Beschrijving voorbeeld" knop - haalt voorbeeld op basis van gebouwType
    btnExample.addEventListener('click', () => {
        const exampleText = getExampleDescription(buildingTypeName);
        textarea.value = exampleText;
        charCounter.textContent = exampleText.length;
        validateDescription();
    });

    // Opslaan knop
    btnSave.addEventListener('click', () => {
        const name = nameInput.value.trim();
        const description = textarea.value.trim();
        
        // Validatie uitvoeren
        const isNameValid = validateName();
        const isDescriptionValid = validateDescription();
        
        // Validatie: naam mag niet leeg zijn
        if (!isNameValid) {
            alert('Voer een naam in voor de polygon.');
            nameInput.focus();
            return;
        }
        
        // Validatie: beschrijving mag niet leeg zijn
        if (!isDescriptionValid) {
            alert('Voer een beschrijving in.');
            textarea.focus();
            return;
        }
        
        closeModal();
        onSave(name, description);
    });

    // Annuleren knop
    btnCancel.addEventListener('click', () => {
        closeModal();
        if (onCancel) onCancel();
    });

    // Modal sluiten hulpfunctie
    function closeModal() {
        overlay.remove();
    }

    // Focus op naam input zetten
    nameInput.focus();
}
