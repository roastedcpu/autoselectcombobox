import { ComboBox } from '@vaadin/combo-box';
import { ComboBoxPlaceholder } from '@vaadin/combo-box/src/vaadin-combo-box-placeholder.js';

class AutoSelectComboBoxElement extends ComboBox {
  constructor() {
    super();
    this.previousInputLabel = '';
    this._triggersCache = null;
    this._listenersAttached = false;
    this._onCustomValueSetBound = this._onCustomValueSet.bind(this);
    this._onValueSetBound = this._onValueSet.bind(this);
    this._onKeyDownBound = this._onCustomKeyDown.bind(this);
    this._onBlurBound = this._onCustomBlur.bind(this);
  }

  static get properties() {
    return {
      /** @protected */
      _invalidInternal: {
        type: Boolean
      },

      /** Set from server — external validation state. */
      invalidExternal: {
        type: Boolean,
        observer: '_invalidExternalChanged'
      },

      /**
       * JSON array of enabled trigger names, e.g. '["ENTER","BLUR"]'.
       * Set from server via setProperty. Empty array = custom values disabled.
       * @private
       */
      _customValueTriggers: {
        type: String,
        value: '[]',
        observer: '_customValueTriggersChanged'
      },

      /**
       * Whether Enter on an unchanged existing value should notify the server.
       * Set from server when an ExistingValueHandler is configured.
       * @private
       */
      _existingValueEnterEnabled: {
        type: Boolean,
        value: false
      }
    };
  }

  static get is() {
    return 'vcf-auto-select-combo-box';
  }

  /** @private */
  _customValueTriggersChanged() {
    this._triggersCache = null;
  }

  /** @private */
  _getParsedTriggers() {
    if (this._triggersCache === null) {
      try {
        this._triggersCache = JSON.parse(this._customValueTriggers || '[]');
      } catch (e) {
        this._triggersCache = [];
      }
    }
    return this._triggersCache;
  }

  /** @private */
  _isTriggerEnabled(name) {
    return this._getParsedTriggers().includes(name);
  }

  /** @private */
  _isCustomValueMode() {
    return this._getParsedTriggers().length > 0;
  }

  // --- Lifecycle ---

  /** @protected */
  ready() {
    super.ready();
    this.allowCustomValue = true;
    this.dirty = this.value !== '';
    this._attachListeners();
  }

  /** @protected */
  connectedCallback() {
    super.connectedCallback();
    this._attachListeners();
  }

  /** @protected */
  disconnectedCallback() {
    super.disconnectedCallback();
    this._detachListeners();
  }

  /** @private */
  _attachListeners() {
    if (this._listenersAttached) {
      return;
    }
    this.addEventListener('custom-value-set', this._onCustomValueSetBound);
    this.addEventListener('value-changed', this._onValueSetBound);
    this.addEventListener('keydown', this._onKeyDownBound);
    this.addEventListener('blur', this._onBlurBound, true);
    this._listenersAttached = true;
  }

  /** @private */
  _detachListeners() {
    if (!this._listenersAttached) {
      return;
    }
    this.removeEventListener('custom-value-set', this._onCustomValueSetBound);
    this.removeEventListener('value-changed', this._onValueSetBound);
    this.removeEventListener('keydown', this._onKeyDownBound);
    this.removeEventListener('blur', this._onBlurBound, true);
    this._listenersAttached = false;
  }

  // --- Trigger handling ---

  /**
   * Unified Enter/Tab handler. On Enter:
   *  - If existing value is selected and text hasn't changed → fires existing-value-enter
   *  - If text doesn't match any item → dispatches custom-value-set
   * On Tab:
   *  - If text doesn't match any item → dispatches custom-value-set
   * @private
   */
  _onCustomKeyDown(e) {
    if (e.key === 'Tab' && this._isTriggerEnabled('TAB')) {
      this._maybeDispatchCustomValue();
      return;
    }

    if (e.key === 'Enter') {
      // Case 1: existing value, text unchanged → edit
      if (this._existingValueEnterEnabled && this.selectedItem && this.value) {
        const currentLabel = this._getItemLabel(this.selectedItem);
        const inputValue = this.inputElement?.value ?? '';
        if (currentLabel === inputValue) {
          this.$server.onExistingValueEnter();
          return;
        }
      }
      // Case 2: no match → custom value (handled by combo's built-in custom-value-set)
      // Nothing to do here — the combo box fires custom-value-set on its own
    }
  }

  /** @private */
  _onCustomBlur() {
    if (!this._isTriggerEnabled('BLUR')) {
      return;
    }
    requestAnimationFrame(() => this._maybeDispatchCustomValue());
  }

  /** @private */
  _maybeDispatchCustomValue() {
    const inputValue = this.inputElement?.value ?? '';
    if (inputValue === '' || this.selectedItem) {
      return;
    }
    this.dispatchEvent(new CustomEvent('custom-value-set', {
      detail: inputValue,
      bubbles: true,
      composed: true
    }));
  }

  // --- Value and validation ---

  /** @private */
  _onValueSet(e) {
    if (!e.detail.value) {
      return;
    }

    if (this.items) {
      this._invalidInternal = this.__getItemIndexByValue(this.items, e.detail.value) < 0;
    } else {
      this._invalidInternal = false;
    }

    if (e.detail.value !== '') {
      this.dirty = true;
    }
    this._updateInvalidState();
  }

  /** @private */
  _onCustomValueSet(e) {
    if (this.previousInputLabel === e.detail) {
      return;
    }
    this.previousInputLabel = e.detail;
    this.dirty = true;

    if (!this._isCustomValueMode()) {
      this.checkValidity();
    }
  }

  /** @protected @override */
  _detectAndDispatchChange() {
    super._detectAndDispatchChange();
    this.previousInputLabel = this._getItemLabel(this.selectedItem);
    this.dirty = true;
    this.checkValidity();
  }

  /** @protected @override */
  _filteredItemsChanged(filteredItems, oldFilteredItems) {
    super._filteredItemsChanged(filteredItems, oldFilteredItems);

    if (!this.filteredItems || this.filteredItems.length !== 1) {
      return;
    }

    const item = this.filteredItems[0];
    if (item instanceof ComboBoxPlaceholder || item.key === undefined) {
      return;
    }
    this._focusedIndex = 0;
  }

  /** @private */
  _invalidExternalChanged() {
    this._updateInvalidState();
  }

  /** @protected @override */
  checkValidity() {
    let validity = super.checkValidity();

    const inputValue = this.inputElement?.value ?? '';

    if (inputValue === '' && !this.required) {
      this._invalidInternal = false;
      this._updateInvalidState();
      this.dirty = false;
      return true;
    }

    if (!this.dirty) {
      return validity;
    }

    if (inputValue === '' && this.required) {
      validity = false;
    } else if (!validity && this._filteredItemsContainValue()) {
      validity = true;
    } else if (validity && !this.selectedItem && !this._filteredItemsContainValue()) {
      if (!this._isCustomValueMode()) {
        validity = false;
      }
    }

    this._invalidInternal = !validity;
    this._updateInvalidState();
    this.dirty = false;
    return validity;
  }

  /** @private */
  _filteredItemsContainValue() {
    if (!this.filteredItems) {
      return false;
    }
    const inputValue = this.inputElement?.value ?? '';
    return this.filteredItems.some(
      item => item.label === this.filter || item.label === inputValue
    );
  }

  /** @private */
  _updateInvalidState() {
    this.invalid = !!(this._invalidInternal || this.invalidExternal);
  }

  /** @protected @override */
  _closeOrCommit() {
    if (!this.filteredItems || this.filteredItems.length !== 1 || this._focusedIndex !== 0) {
      super._closeOrCommit();
      return;
    }

    const focusedItem = this.filteredItems[0];
    if (focusedItem instanceof ComboBoxPlaceholder) {
      this.close();
    } else {
      super._closeOrCommit();
    }
  }
}

customElements.define(AutoSelectComboBoxElement.is, AutoSelectComboBoxElement);
