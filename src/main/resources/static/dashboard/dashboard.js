import { DashboardService } from "./dashboard-service";

let service = new DashboardService();

export class DashboardElement extends HTMLElement {

    constructor() {
        super();
    }

    async connectedCallback() {
        const shadow = this.attachShadow({mode: 'open'});

        const wrapper = document.createElement("div");

        try {
            const jsonData = await service.getMeasurements();
            wrapper.innerText = JSON.stringify(jsonData, null, 2);
        } catch (error) {
            console.error('Error fetching measurements:', error);
            wrapper.innerText = 'Failed to fetch measurements.';
        }

        shadow.appendChild(wrapper);
    }

}

customElements.define('dashboard', DashboardElement);