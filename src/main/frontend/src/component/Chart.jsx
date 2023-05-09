// -----------------------------------------------------------
//
// Chart
//
// Display chart
//
// -----------------------------------------------------------
import React from 'react';
import {BarElement, CategoryScale, Chart as ChartJS, Legend, LinearScale, Title, Tooltip} from 'chart.js'
import {Bar, Doughnut} from 'react-chartjs-2';

ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement, Title, Tooltip, Legend
)
const backgroundColor = [
  'rgba(255, 99, 132, 0.2)',
  'rgba(54, 162, 235, 0.2)',
  'rgba(255, 206, 86, 0.2)',
  'rgba(75, 192, 192, 0.2)',
  'rgba(153, 102, 255, 0.2)',
  'rgba(255, 159, 64, 0.2)',
];
const borderColor = [
  'rgba(255, 99, 132, 1)',
  'rgba(54, 162, 235, 1)',
  'rgba(255, 206, 86, 1)',
  'rgba(75, 192, 192, 1)',
  'rgba(153, 102, 255, 1)',
  'rgba(255, 159, 64, 1)',
];


const oneBackgroundColor = [
  'rgba(255, 99, 132, 0.2)'
];
const oneBorderColor = [
  'rgba(255, 99, 132, 1)'
];

class Chart extends React.Component {


  /** Caller must declare  this.props.changePasswordCallback(isCorrect, password ) */
  constructor(props) {
    super();
    this.state = {
      type: props.type,
      title: props.title,
      labels: props.labels,
      data: props.data,
      dataMap: props.dataMap,
      dataList: props.dataList,
      oneColor: props.oneColor,
      options: props.options
    };

    /** Default options value */
    if (this.state.options.title === undefined) {
      this.state.options.title = true
    }

    // console.log("Chart.constructor: dataMap=" + JSON.stringify(props.dataMap))
    this.getLabelsDataFromMap = this.getLabelsDataFromMap.bind(this);
  }

// Calculate the state to display
  componentDidUpdate(prevProps) {
    // console.log("Chart.componentDidUpdate: prevProps=" + JSON.stringify(prevProps))
    if (prevProps.dataMap !== this.props.dataMap) {
      this.setState({
        dataMap: this.props.dataMap
      });
    }
    if (prevProps.dataList !== this.props.dataList) {
      this.setState({
        dataList: this.props.dataList
      });
    }
    if (prevProps.data !== this.props.data) {
      this.setState({
        data: this.props.data
      });
    }
    if (prevProps.labels !== this.props.labels) {
      this.setState({
        labels: this.props.labels
      });
    }
  }


  render() {
    // console.log("Chart.render: type=" + this.state.type + " state=" + JSON.stringify(this.state))


    let htmlTitle = "";

    let mainStyles = {};

    if (this.state.options.border === true) {
      mainStyles = {borderRadius: "10px", border: "3px solid rgb(222, 203, 228)"};
    }
    if (this.state.options.title === true) {
      htmlTitle = (<div class="h6" style={{textAlign: "center"}}> {this.state.title} </div>)
    }
    // ------------------------------------------- Doughnut

    return (<div style={mainStyles}>
        {htmlTitle}
        {this.state.type === 'Doughnut' &&
          <Doughnut data={this.getDataDoughnutChart()} options={this.getOptionsDoughnutChart()}
                    width={this.state.options.width}
                    height={this.state.options.height}/>}
        {(this.state.type === 'VerticalBar' || this.state.type === 'HorizontalBar') &&
          <Bar data={this.getDataBarChart()} options={this.getOptionsBarChart()}
               width={this.state.options.width}
               height={this.state.options.height}/>}
      </div>
    )


  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  BAR                                                                 */
  /*                                                                      */

  /* ******************************************************************** */


  getOptionsBarChart() {
    const chartOptions = {
      'maintainAspectRatio': false,
      'responsive': false,
      'plugins': {
        'legend': {
          'display': false
        },
      },
    };
    if (this.state.type === 'HorizontalBar') {
      chartOptions.scales = {
        y: {
          ticks: {
            beginAtZero: true,
            display: true
          },
          grid: {
            display: false,
          }
        },
        x: {
          ticks: {
            display: true,
          },
          grid: {
            display: false,
          }
        },
      };
    }
    if (this.state.type === 'VerticalBar') {
      chartOptions.indexAxis = 'y';
      chartOptions.elements = {
        bar: {
          borderWidth: 2,
        },
      };
      chartOptions.plugins = {
        legend: {
          position: 'right',
        },
        title: {
          display: false,
        },
      };
    }
    if (this.state.options.showGrid === false) {
      chartOptions.scales.x.grid.display = false;
      chartOptions.scales.y.grid.display = false;

    }

    if (this.state.options.width !== undefined || this.state.options.height !== undefined) {
      chartOptions.maintainAspectRatio = false;
    }
    // value is true by default
    if (this.state.options.showXLabel === false) {
      chartOptions.scales.x.ticks.display = false;
    }
    if (this.state.options.showYLabel === false) {
      chartOptions.scales.y.ticks.display = false;
    }

    return chartOptions;
  }


  getDataBarChart() {
    const dataChart = {};

    dataChart.datasets = [];
    dataChart.datasets.label = this.state.title;


    let dataset = {};
    dataChart.datasets.push(dataset);
    if (this.state.oneColor) {
      dataset.backgroundColor = oneBackgroundColor;
      dataset.borderColor = oneBorderColor;
    } else {
      dataset.backgroundColor = backgroundColor;
      dataset.borderColor = borderColor;
    }
    dataset.borderWidth = 1;

    if (this.state.dataList) {
      const resultTransformed = this.getLabelsDataFromList(this.state.dataList);
      dataChart.labels = resultTransformed.labels;
      dataset.data = resultTransformed.data;
    }
    return dataChart;
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  DOUGHNUT                                                                 */
  /*                                                                      */

  /* ******************************************************************** */


  getOptionsDoughnutChart() {
    const chartOptions = {
      'plugins': {
        'legend': {
          'display': false
        }
      }
    }
    if (this.state.options.width !== undefined || this.state.options.height !== undefined) {
      chartOptions.maintainAspectRatio = false;
    }

    return chartOptions;
  }


  getDataDoughnutChart() {
    const dataChart = {};

    dataChart.datasets = [];
    dataChart.datasets.label = this.state.title;

    let dataset = {};
    dataChart.datasets.push(dataset);
    dataset.backgroundColor = backgroundColor;
    dataset.borderColor = borderColor;
    dataset.borderWidth = 1;
    if (this.state.dataMap) {
      const resultTransformed = this.getLabelsDataFromMap(this.state.dataMap);
      dataChart.labels = resultTransformed.labels;
      dataset.data = resultTransformed.data;
    } else if (this.state.labels) {
      dataChart.labels = this.state.labels;
      dataset.data = this.state.data;
    }
    console.log("Chart: dataChart=" + JSON.stringify(dataChart));
    return dataChart;
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  Conversion function                                                     */
  /*                                                                      */

  /* ******************************************************************** */

  /**
   * value {admin: 3, user: 10, trans: 0}
   * returns:
   * - labels= [ 'admin', 'user', 'trans' ]
   * - data= [ 3, 10, 0
   */
  getLabelsDataFromMap(dataMap) {
    const result = {
      'labels': [],
      'data': []
    };
    for (const [key, value] of Object.entries(dataMap)) {
      result.labels.push(key)
      result.data.push(value)
    }
    // console.log("Chart:getLabelsDataFromMap : "+result+" from "+dataMap);
    return result;
  }

  /*
  * value [{value: 41, label: "2021-10-14"}, {value: 2, label: "2021-10-15"}, {value: 8, label: "2021-10-16"},…]
  * returns:
  * - labels= [ "2021-10-14", "2021-10-15", "2021-10-16", ... ]
  * - data= [ 41, 2, 8,...
  */
  getLabelsDataFromList(dataList) {
    const result = {
      'labels': [],
      'data': []
    };
    for (let i in dataList) {
      result.labels.push(dataList[i].label)
      result.data.push(dataList[i].value)
    }
    // console.log("Chart:getLabelsDataFromList : " + result + " from " + dataList);
    return result;
  }
}

export default Chart;


/* target Doughnut*/
