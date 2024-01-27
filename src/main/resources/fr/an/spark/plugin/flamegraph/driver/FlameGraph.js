function onClick() {
    console.log("onClick");
    const xhr = new XMLHttpRequest();
    xhr.open("GET", "/flameGraph/refresh");
    xhr.send();
    xhr.responseType = "json";
    xhr.onload = () => {
        if (xhr.readyState == 4 && xhr.status == 200) {
            const data = xhr.response;
            console.log(data);
        } else {
            console.log(`Error: ${xhr.status}`);
        }
    };
}