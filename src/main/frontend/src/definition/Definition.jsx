// -----------------------------------------------------------
//
// Definition
//
// List of all runners available
//
// -----------------------------------------------------------

import React from 'react';
import {Button, Modal} from "react-bootstrap";

import RestCallService from "../services/RestCallService";
import RunnerDefinition from "./RunnerDefinition";

class Definition extends React.Component {


  constructor(_props) {
    super();
    this.state = {runners: [], isOpen: false};
    this.openModal = this.openModal.bind(this);
    this.closeModal = this.closeModal.bind(this);
  }

  componentDidMount() {
    this.refreshList();
  }

  /*           {JSON.stringify(this.state.runners, null, 2) } */
  render() {
    return (<div>
      <div className="container">
        <h1 className="title">Zeebe Connectors</h1>

        <div className="block">
          <table id="runnersTable" className="table is-hoverable is-fullwidth">
            <thead>
            <tr>
              <th>Runner Name</th>
              <th>Type</th>
              <th>Type Runner</th>
              <th>Class Name</th>
            </tr>
            </thead>
            <tbody>
            {this.state.runners ? this.state.runners.map((runner, _index) =>
              <tr onClick={() => this.openModal(runner)}>
                <td>
                  <img style={{width: "30px"}} src={runner.logo} alt="logo runner"/>
                  &nbsp;
                  {runner.name}</td>
                <td>{runner.type}</td>
                <td>{runner.typeRunner}</td>
                <td>{runner.className}</td>
              </tr>
            ) : <tr/>}
            </tbody>
          </table>
        </div>
      </div>

      <div style={{paddingTop: "30px"}}>
        <div className="card" style={{width: "25rem;"}}>
          <div className="card-header" style={{backgroundColor: "rgba(0,0,0,.03)"}}>Element template</div>
          <div className="card-body">

            <div className="block" style={{paddingTop: "10px"}}>
              <a target="top" href={this.getDownloadUrl()}
                 className="button is-info"
                 download>Download Collection Element Template</a>
            </div>
            <p><i>Download the complete collection and place it in the Desktop modeler to access in your process all
              templates</i></p>
          </div>
        </div>
      </div>

      <Modal show={this.state.isOpen} onHide={this.closeModal}
             dialogClassName="modal-90w"
             aria-labelledby="example-custom-modal-styling-title"
             backdrop="static"
      >
        <Modal.Header closeButton>
          <Modal.Title>{this.state.currentrunner && this.state.currentrunner.name}</Modal.Title>
        </Modal.Header>
        <Modal.Body><RunnerDefinition runner={this.state.currentrunner}/></Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={this.closeModal}>
            Close
          </Button>
        </Modal.Footer>
      </Modal>

    </div>);
  }


  refreshList() {
    console.log("Definition.refreshList http[cherry/api/runner/list]");
    this.setState({runners: [], status: ""});
    var restCallService = RestCallService.getInstance();
    restCallService.getJson('cherry/api/runner/list?', this, this.refreshListCallback);
  }

  refreshListCallback(httpPayload) {
    if (httpPayload.isError()) {
      this.setState({status: "Error"});
    } else {
      this.setState({runners: httpPayload.getData()});

    }
  }

  openModal(runner) {
    console.log("Open modal");

    this.setState({isOpen: true, currentrunner: runner})
  }

  closeModal() {
    console.log("Close modal");
    this.setState({isOpen: false})
  }

  getDownloadUrl() {
    let restCallService = RestCallService.getInstance();
    let urlServer = restCallService.getUrlServer();
    return urlServer + "/cherry/api/runner/templatefile?";
  }
}

export default Definition;
