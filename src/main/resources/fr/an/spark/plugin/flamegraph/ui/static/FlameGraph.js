/* global d3, flamegraph */

const dummyFlameGraphData = {
    "name": "root", "value": 100,
    "children": [
        {
            "name": "f1", value: 40,
            "children": [
                {
                    "name": "g1", value: 39,
                    "children": [
                            {
                                "name": "h1", value: 37
                            }
                   ]
                }
            ]
        },
        {
            "name": "f2", value: 40
        }
    ]
};
function onClickDummyFlameGraph() {
    drawFlamegraphAt("#flamegraph-chart1", dummyFlameGraphData);
}

function onClickRefreshFlameGraph() {
    console.log("onClickRefreshFlameGraph");
    const xhr = new XMLHttpRequest();
    xhr.open("GET", "/flamegraph-plugin/api/v1/flameGraph");
    xhr.send();
    xhr.responseType = "json";
    xhr.onload = () => {
        if (xhr.readyState == 4 && xhr.status == 200) {
            const data = xhr.response;
            console.log(data);
            drawFlamegraphAt("#flamegraph-chart1", data);
        } else {
            console.log(`Error: ${xhr.status}`);
        }
    };
}


function drawFlamegraphAt(elementSelector, jsonData) {
    const chart = flamegraph()
        // .width((window.innerWidth * 95) / 100)
        .cellHeight(18)
        // .transitionEase(d3.easeCubic)
        // .sort(true)
        // .title("")
        ;
    d3.select(elementSelector)
        .datum(jsonData)
        .call(chart);

    // window.onresize = () => chart.width((window.innerWidth * 95) / 100);
}

function bindFlamegraphToggle(elementHeaderSelector, elementChartSelector) {
    d3.select(elementHeaderSelector).on("click", () => {
        const arrow = d3.select("#executor-flamegraph-arrow");
        arrow
            .classed("arrow-open", !arrow.classed("arrow-open"))
            .classed("arrow-closed", !arrow.classed("arrow-closed"));

        if (arrow.classed("arrow-open")) {
            d3.select(elementChartSelector).style("display", "block");
        } else {
            d3.select(elementChartSelector).style("display", "none");
        }
    });
}
