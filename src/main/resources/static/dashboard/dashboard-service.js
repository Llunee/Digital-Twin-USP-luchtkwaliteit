export class DashboardService {
    async getMeasurements() {
        const url = "/measurements";

        console.log("going to fetch!");
        return fetch(url)
            .then(response => {
                const datadiv = document.querySelector("#app");
                console.log(response.json());
                console.log("done");
            })
            .catch(error => {
                console.log(error);
            })
    }
}