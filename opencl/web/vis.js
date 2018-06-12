console.log(d3);
const svg = d3.select("svg");
const width = svg.attr("width");
const height = svg.attr("height");

d3.json("http://localhost:8080", {crossOrigin: "no-cors"}).then(function (grid) {
  console.log(grid);

  const {nx, ny, dx, dy, mesh} = grid;
  const x = nx * dx;
  const y = ny * dy;

  const xScale = d3.scaleLinear()
      .domain([-2 * dx, nx + 2 * dx])
      .range([0, width]);

  const yScale = d3.scaleLinear()
      .domain([-2 * dy, ny + 2 * dy])
      .range([height, 0]);

  const meshLineGen = d3.line()
      .x(segment => xScale(segment.a.x))
      .y(segment => yScale(segment.a.y));
  console.log(meshLineGen);
  console.log(meshLineGen.curve(d3.curveLinearClosed));

  console.log(mesh.segments);
  svg.datum(mesh.segments);
  svg
      .append("path")
      .attr("class", "mesh")
      .attr("d", meshLineGen)
      .attr("stroke", "steelblue")
      .attr("stroke-width", 1.5)
      .attr("fill", "none");
});