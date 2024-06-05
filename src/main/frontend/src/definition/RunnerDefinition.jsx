// -----------------------------------------------------------
//
// RunnerDefinition
//
// Manage the main application
//
// -----------------------------------------------------------

import React from 'react';
import {Card, Tab, Tabs} from 'react-bootstrap';
import RestCallService from "../services/RestCallService";


class RunnerDefinition extends React.Component {


  constructor(props) {
    super();
    this.state = {runner: props.runner};
  }


  render() {
    return (
      <div>
        <table style={{marginBottom: "10px"}}>
          <tbody>
          <tr>
            <td>
              <div style={{border: "1px solid", padding: "5px", textAlign: "center", boxShadow: "5px 5px"}}>
                <img style={{width: "50px"}} src={this.state.runner.logo} alt="logo"/>
              </div>
            </td>
            <td style={{paddingLeft: "30px"}}>
              {this.state.runner.label}
            </td>
          </tr>
          </tbody>
        </table>

        <Tabs defaultActiveKey="description">
          <Tab eventKey="description" title="Description">
            <table style={{width: "25rem", padding: "10px"}}>
              <tr>
                <td style={{verticalAlign: "top", padding: "10px"}}>
                  <Card>
                    <Card.Header style={{backgroundColor: "rgba(0,0,0,.03)"}}>{this.state.runner.name}</Card.Header>
                    <Card.Body>

                      <Card.Title>Type</Card.Title>
                      <Card.Text>{this.state.runner.type}</Card.Text>

                      <Card.Title>Description</Card.Title>
                      <Card.Text>{this.state.runner.description}</Card.Text>

                      <Card.Title>Class Name:</Card.Title>
                      <Card.Text>{this.state.runner.className}</Card.Text>

                      <Card.Title>Class Name</Card.Title>
                      <Card.Text>{this.state.runner.className}</Card.Text>

                      <Card.Title>Validation:</Card.Title>
                      <Card.Text>
                        <p className="card-text" style={{
                          paddingLeft: "30px",
                          paddingBottom: "20px",
                          color: "red"
                        }}>{this.state.runner.validationErrorsMessage}</p>
                      </Card.Text>

                    </Card.Body>
                  </Card>
                </td>
                <td style={{verticalAlign: "top", padding: "10px"}}>
                  <Card>
                    <Card.Header style={{backgroundColor: "rgba(0,0,0,.03)"}}>Element template</Card.Header>
                    <Card.Body>

                      <a target="top" href={this.getDownloadUrlRunner(this.state.runner)}
                         className="button is-info"
                         download>Download {this.state.runner.name} Element Template</a>

                      <p class="card-text" style={{paddingLeft: "30px", paddingBottom: "20px"}}><i>Copy the JSON in the
                        WebModeler to access the definition as a template</i></p>
                    </Card.Body>
                  </Card>

                </td>
              </tr>
            </table>
          </Tab>
          <Tab eventKey="Inputs" title="Input">
            <table className="table">
              <thead>
              <tr>
                <th>Name</th>
                <th>Description</th>
                <th>Class</th>
                <th>Default</th>
                <th>Level</th>
              </tr>
              </thead>
              <tbody>
              {this.state.runner.listInput ? this.state.runner.listInput.map((input, _index) =>
                <tr>
                  <td>{input.name}</td>
                  <td>{input.explanation}</td>
                  <td>{input.clazz}</td>
                  <td>{input.defaultValue}</td>
                  <td>{input.level}</td>
                </tr>) : <div/>}

              </tbody>
            </table>
          </Tab>
          <Tab eventKey="Output" title="Outputs">
            <table className="table">
              <thead>
              <tr>
                <th>Name</th>
                <th>Description</th>
                <th>Class</th>
                <th>Default</th>
                <th>Level</th>
              </tr>
              </thead>
              <tbody>
              {this.state.runner.listOutput ? this.state.runner.listOutput.map((output, _index) =>
                <tr>
                  <td>{output.name}</td>
                  <td>{output.explanation}</td>
                  <td>{output.clazz}</td>
                  <td>{output.defaultValue}</td>
                  <td>{output.level}</td>
                </tr>) : <div/>}

              </tbody>
            </table>
          </Tab>
          <Tab eventKey="errors" title="Errors">
            <table className="table">
              <thead>
              <tr>
                <th>Name</th>
                <th>Explanation</th>
              </tr>
              </thead>
              <tbody>
              {this.state.runner.listBpmnErrors ? this.state.runner.listBpmnErrors.map((error, _index) =>
                <tr>
                  <td>{error.code}</td>
                  <td>{error.explanation}</td>
                </tr>) : <div/>}

              </tbody>
            </table>
          </Tab>
        </Tabs>


      </div>);
  }

  getDownloadUrlRunner(runner) {
    let restCallService = RestCallService.getInstance();
    let urlServer = restCallService.getUrlServer();
    console.log("GetURL Download runner ["+runner.name+"] : ["+urlServer + "/cherry/api/runner/templatefile?name=" + runner.name)
    return urlServer + "/cherry/api/runner/templatefile?name=" + runner.name;
  }
}

export default RunnerDefinition;
