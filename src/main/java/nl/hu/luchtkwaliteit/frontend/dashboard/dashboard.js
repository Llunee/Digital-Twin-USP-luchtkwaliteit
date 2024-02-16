import { LitElement, html, css } from "lit";

export class DashboardElement extends LitElement {

    static get properties() {
        return {}
    }

    constructor() {
        super();
    }

    render() {
        return (
            <div>
            </div>
        );
    }

    static get styles() {
        return css``
    }

}

customElements.define('dashboard', DashboardElement);