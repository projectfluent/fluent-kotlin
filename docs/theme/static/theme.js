export class Sidebar extends HTMLElement {
    constructor() {
        super()
        this.innerHTML = 'Sidebar created'
    }
    connectedCallback() {
        this.innerHTML = '';
        let toc = document.body.querySelector('sphinx-content div.toctree-wrapper');
        if (!toc) {
            toc = document.getElementById('toctree').content;
        }
        if (toc) {
            this.appendChild(toc.cloneNode(true));
        }
    }
}

const doc_shadow = (function() {
    let template = document.createElement('template');
    template.innerHTML = `
<style>
:host {
    display: grid;
    grid-template-columns: 0 auto 1fr ;
}
</style>
<slot />
    `;
    return template.content;
})();

export class Document extends HTMLElement {
    constructor() {
        super()
        this.attachShadow({mode: 'open'}).appendChild(doc_shadow.cloneNode(true));
    }
}

customElements.define('sphinx-document', Document);
customElements.define('sphinx-sidebar', Sidebar);
