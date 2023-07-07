// -----------------------------------------------------------
//
// RunnerDashboard
//
// Dashboard for a runner: show overciew, logs, errors, statistics
//
// -----------------------------------------------------------

import React from 'react';
import {Button, Modal} from "react-bootstrap";
import RunnerMonitoring from "./RunnerMonitoring";
import RunnerHeader from "../definition/RunnerHeader";
import {Tag} from "carbon-components-react";

class RunnerDashboard extends React.Component {


  constructor(props) {
    super();

    this.state = {runner: props.runnerDisplay,
      timestamp: props.timestamp,
      isOpen: false};
    this.openModal = this.openModal.bind(this);
    this.closeModal = this.closeModal.bind(this);

  }

  componentDidUpdate (prevProps) {
    if (prevProps.timestamp !== this.props.timestamp) {
      this.setState({
        runner: this.props.runnerDisplay,
        timestamp: this.props.timestamp
      });
    }
  }

  render() {
    return (
      <div >
        <div  bis_skin_checked="1">
          <table style= {{width: "100%" }}>
            <tbody>
            <tr onClick={()=>this.openModal()}>
              <td style={{padding: "10px", margin: "20px"}}>
                <div className="column is-two-thirds" style={{padding: "0px 0px 20px 0px"}} bis_skin_checked="1">
                  <h1 className="title is-5 job-type is-hoverable">
                    <table>
                      <tbody>
                      <tr>
                        <td >
                          <table style={{border: "1px solid", padding: "5px", textAlign: "center", boxShadow: "5px 5px"}}>
                            <tr><td><img width="30px" src={this.state.runner.logo} alt={this.state.runner.name}/></td></tr>
                            <tr><td style={{border: "1px solid", padding: "0px"}}>
                              {this.state.runner.active &&
                              <button className="start-runner button is-selected is-primary"
                                      style={{height: "1.5em", border: "1px solid"}}>Active</button>}
                              {!this.state.runner.active &&
                                <button className="stop-runner button is-selected is-danger"
                                        style={{height: "1.5em"}}>Stopped</button>}
                            </td></tr>
                          </table>

                        </td>
                        <td style={{padding: "5px 0px 0px 25px"}}> {this.state.runner.name}
                          {this.state.runner.statistic && this.state.runner.statistic.executionsFailed > 0 && <Tag type="red" title="Fails"> {this.state.runner.statistic.executionsFailed} Fails </Tag>}
                          {this.state.runner.statistic && this.state.runner.statistic.executionsBpmnErrors > 0 && <Tag type="red" title="BPMN ControllerPage"> {this.state.runner.statistic.executionsBpmnErrors} BPMN Errors </Tag>}
                          {this.state.runner.nboverthreshold > 0 && <Tag type="purple" title="Over threshold"> {this.state.runner.nboverthreshold} Errors </Tag>}
                          <br/>
                          <br/>
                          {this.state.runner.statistic && this.state.runner.statistic.executionsSucceeded} Successful,
                          {this.state.runner.statistic && this.state.runner.statistic.executionsFailed} failed,
                          {this.state.runner.statistic && this.state.runner.statistic.executionsBpmnErrors} BPMN Errors<br/>

                          <span style={{fontStyle: "italic", fontSize: "small"}}>{this.state.runner.collectionname}&nbsp;({this.state.runner.classrunner})</span>

                        </td>
                      </tr>

                      </tbody>
                    </table>
                  </h1>
                  <h1>
                    <progress className="progress is-small is-primary" value="1" max="1"></progress>
                  </h1>
                  <h2 style={{padding: "5px 0px 0px 25px", fontSize: "70%"}}>
                    <i >Average {this.state.runner.performance.averageTimeInMs} ms
                      Peak {this.state.runner.performance.peakTimeInMs} ms </i></h2></div>
              </td>
            </tr>
            </tbody>
          </table>

        </div>

        <Modal show={this.state.isOpen} onHide={this.closeModal}
               dialogClassName="modal-90w"
               aria-labelledby="example-custom-modal-styling-title"
               backdrop="static"
        >
          <Modal.Header closeButton>
            <Modal.Title>
              <RunnerHeader runner={this.state.runner}/>
            </Modal.Title>
          </Modal.Header>
          <Modal.Body><RunnerMonitoring runnerDisplay={this.state.runner} timestamp={this.state.timestamp}/></Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={this.closeModal}>
              Close
            </Button>
          </Modal.Footer>
        </Modal>
      </div>);
  }

  openModal ( ) {
    console.log("Open modal");

    this.setState({ isOpen: true})
  }


  closeModal () {
    console.log("Close modal");
    this.setState({ isOpen: false })
  }

}

export default RunnerDashboard;
