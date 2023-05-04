// -----------------------------------------------------------
//
// Parameters
//
// List of all runners available
//
// -----------------------------------------------------------

import React from 'react';
import {Button, NumberInput, Select, TextInput} from "carbon-components-react";
import {ArrowRepeat} from "react-bootstrap-icons";
import ControllerPage from "../component/ControllerPage";
import RestCallService from "../services/RestCallService";

class Parameters extends React.Component {


  constructor(_props) {
    super();
    this.state = {
      parameters: {},
      display: {loading: false}
    };
  }

  componentDidMount() {
    console.log("Definition.componentDidMount: BEGIN");
    this.refreshListParameters();
    console.log("Definition.componentDidMount: END");
  }

  /*           {JSON.stringify(this.state.runners, null, 2) } */
  render() {
    return (
      <div className={"container"}>
        <div className="row" style={{width: "100%"}}>
          <div className="col-md-10">
            <h1 className="title">Parameters</h1>
          </div>
          <div className="col-md-2">
            <Button className="btn btn-success btn-sm"
                    onClick={() => this.refreshListParameters()}
                    disabled={this.state.display.loading}>
              <ArrowRepeat/> Refresh
            </Button>
          </div>
        </div>
        <div className="row" style={{width: "100%"}}>
          <div className="col-md-12">
            <ControllerPage errorMessage={this.state.status} loading={this.state.display.loading}/>
          </div>
        </div>


        <h4>Zeebe Connection</h4>

        <div className="row">
          <div className="col-md-4">
            <Select
              value={this.state.parameters.zeebekindconnection}
              labelText="Zeebe connection"
              disabled="true"
              readonly="true"
              onChange={(event) => this.setParameterProperty("zeebekindconnection", event.target.value)}>
              <option value="GATEWAY">Gateway</option>
              <option value="SAAS">Saas</option>
            </Select>
          </div>
        </div>

        <div style={this.getZssZeebeConnection("GATEWAY")}>
          <div className="row">
            <div className="col-md-12">
              <TextInput labelText="Address"
                         readonly="true"
                         style={{width: "200px"}}
                         value={this.state.parameters.gatewayaddress}
                         onChange={(event) => this.setParameterProperty("gatewayaddress", event.target.value)}/>
            </div>
          </div>
          <div className="row">
            <div className="col-md-4">
              <Select
                value={this.state.parameters.plaintext}
                labelText="Security plain text"
                disabled="true"
                readonly="true"
                onChange={(event) => this.setParameterProperty("plaintext", event.target.value)}>
                <option value="true">True</option>
                <option value="false">False</option>
              </Select>
            </div>
          </div>
        </div>


        <div style={this.getZssZeebeConnection("SAAS")}>
          <div className="row">
            <div className="col-md-6">
              <TextInput labelText="Region"
                         readonly="true"
                         value={this.state.parameters.cloudregion}
                         style={{width: "100px"}}
                         onChange={(event) => this.setParameterProperty("cloudregion", event.target.value)}/>
            </div>
          </div>
          <div className="row">
            <div className="col-md-6">
              <TextInput labelText="Cluster ID"
                         readonly="true"
                         value={this.state.parameters.cloudclusterid}
                         style={{width: "200px"}}
                         onChange={(event) => this.setParameterProperty("cloudclusterid", event.target.value)}/>
            </div>
          </div>
          <div className="row">
            <div className="col-md-6">
              <TextInput labelText="client ID"
                         readonly="true"
                         value={this.state.parameters.cloudclientid}
                         style={{width: "300px"}}
                         onChange={(event) => this.setParameterProperty("cloudclientid", event.target.value)}/>
            </div>
          </div>
          <div className="row">
            <div className="col-md-6">
              <TextInput labelText="Client secret"
                         readonly="true"
                         style={{width: "300px"}}
                         value={this.state.parameters.cloudclientsecret}
                         onChange={(event) => this.setParameterProperty("cloudclientsecret", event.target.value)}/>
            </div>
          </div>
        </div>


        <h4>Workers</h4>
        <div className="row">
          <div className="col-md-12">
            <NumberInput label="Threads"
                         readonly="true"
                         min="1"
                         size="sm"
                         value={this.state.parameters.maxjobsactive}
                         style={{width: "100px"}}
                         onChange={(event) => this.setParameterProperty("maxjobsactive", event.target.value)}/>
          </div>
        </div>
        <div className="row">
          <div className="col-md-12">
            <NumberInput label="Threads"
                         readonly="true"
                         min="1"
                         size="sm"
                         value={this.state.parameters.nbthreads}
                         style={{width: "100px"}}
                         onChange={(event) => this.setParameterProperty("nbthreads", event.target.value)}/>
          </div>
        </div>

      </div>
    )

  }

  getZssZeebeConnection(value) {
    if (value === this.state.parameters.zeebekindconnection)
      return {}
    return {display: "none"};

  }


  /**
   * Set a parameter property property
   * @param propertyName name of the property in parameters
   * @param propertyValue the value
   */
  setParameterProperty(propertyName, propertyValue) {
    let parameters = this.state.parameters;
    parameters[propertyName] = propertyValue;
    this.setState({parameters: parameters});
  }


  /**
   * Set the display property
   * @param propertyName name of the property
   * @param propertyValue the value
   */
  setDisplayProperty(propertyName, propertyValue) {
    let displayObject = this.state.display;
    displayObject[propertyName] = propertyValue;
    this.setState({display: displayObject});
  }


  refreshListParameters() {
    let uri = 'cherry/api/runtime/parameters?';
    console.log("parameters.refreshListContent http[" + uri + "]");
    this.setDisplayProperty("loading", true);
    this.setState({status: ""});
    var restCallService = RestCallService.getInstance();
    restCallService.getJson(uri, this, this.refreshListParametersCallback);
  }

  refreshListParametersCallback(httpPayload) {
    this.setDisplayProperty("loading", false);
    if (httpPayload.isError()) {
      console.log("parameters.refreshListContentCallback: error " + httpPayload.getError());
      this.setState({status: httpPayload.getError()});
    } else {
      this.setState({parameters: httpPayload.getData()});
    }
  }
}

export default Parameters;