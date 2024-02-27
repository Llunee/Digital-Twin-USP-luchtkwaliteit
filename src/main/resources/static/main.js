import { Router } from '@vaadin/router';
import { DashboardElement } from "./dashboard/dashboard";

const BASE = import.meta.env.BASE_URL

window.onload = function() {

    const outlet = document.querySelector('#app')
    const router = new Router(outlet)

    router.setRoutes([
        {path: `${BASE}`, component: 'dashboard'},
    ])
}