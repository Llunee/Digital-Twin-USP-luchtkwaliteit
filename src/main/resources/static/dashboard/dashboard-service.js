export class DashboardService {
    async getMeasurements() {
        const url = "/measurements";

        return fetch(url)
            .then(response => response.json())
    }
}