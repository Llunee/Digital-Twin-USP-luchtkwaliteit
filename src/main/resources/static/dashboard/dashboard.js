import { LitElement, html, css } from "lit";
import { DashboardService } from "./dashboard-service";

let service = new DashboardService();

export class DashboardElement extends LitElement {

    static get properties() {
        return {}
    }

    constructor() {
        super();
    }

    render() {
        return html`
            <div>
                ${service.getMeasurements()}
            </div>
        `
    }

    static get styles() {
        return css``
    }

}

customElements.define('dashboard', DashboardElement);