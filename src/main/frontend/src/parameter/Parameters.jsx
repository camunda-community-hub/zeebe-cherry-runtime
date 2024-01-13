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


        <div className="row" >
          <div className="col-md-6">

            <div className="card" style={{width: "25rem;"}}>
              <div className="card-header" style={{backgroundColor: "rgba(0,0,0,.03)"}}>Zeebe connection</div>
              <div className="card-body">


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

                <div style={this.getZssZeebeConnection("DIRECTIPADDRESS")}>
                  <div className="row">
                    <div className="col-md-12">
                      <TextInput labelText="Gateway Address"
                                 readonly="true"
                                 style={{width: "200px"}}
                                 value={this.state.parameters.gatewayAddress}
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


                <div style={this.getZssZeebeConnection("IDENTITY")}>
                  <div className="row">
                    <div className="col-md-12">
                      <TextInput labelText="Address"
                                 readonly="true"
                                 style={{width: "200px"}}
                                 value={this.state.parameters.gatewayAddress}
                                 onChange={(event) => this.setParameterProperty("gatewayAddress", event.target.value)}/>
                    </div>
                  </div>
                  <div className="row">
                    <div className="col-md-12">
                      <TextInput labelText="clientId"
                                 readonly="true"
                                 style={{width: "200px"}}
                                 value={this.state.parameters.clientId}
                                 onChange={(event) => this.setParameterProperty("clientId", event.target.value)}/>
                    </div>
                  </div>
                  <div className="row">
                    <div className="col-md-12">
                      <TextInput labelText="clientSecret"
                                 readonly="true"
                                 style={{width: "200px"}}
                                 value={this.state.parameters.clientSecret}
                                 onChange={(event) => this.setParameterProperty("clientSecret", event.target.value)}/>

                    </div>
                  </div>
                  <div className="row">
                    <div className="col-md-12">
                      <TextInput labelText="Autorization Server Url"
                                 readonly="true"
                                 style={{width: "200px"}}
                                 value={this.state.parameters.AutorizationServerUrl}
                                 onChange={(event) => this.setParameterProperty("AutorizationServerUrl", event.target.value)}/>

                    </div>
                  </div>
                  <div className="row">
                    <div className="col-md-12">
                      <TextInput labelText="client Audience"
                                 readonly="true"
                                 style={{width: "200px"}}
                                 value={this.state.parameters.clientAudience}
                                 onChange={(event) => this.setParameterProperty("clientAudience", event.target.value)}/>
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
                  <div className="row">
                    <div className="col-md-4">
                      <TextInput labelText="Address"
                                 readonly="true"
                                 style={{width: "200px"}}
                                 value={this.state.parameters.tenantIds}
                                 onChange={(event) => this.setParameterProperty("gatewayaddress", event.target.value)}/>

                    </div>
                  </div>

                </div>




                <div style={this.getZssZeebeConnection("SAAS")}>
                  <div className="row">
                    <div className="col-md-6">
                      <TextInput labelText="Region"
                                 readonly="true"
                                 value={this.state.parameters.cloudRegion}
                                 style={{width: "100px"}}
                                 onChange={(event) => this.setParameterProperty("cloudRegion", event.target.value)}/>
                    </div>
                  </div>
                  <div className="row">
                    <div className="col-md-6">
                      <TextInput labelText="Cluster ID"
                                 readonly="true"
                                 value={this.state.parameters.cloudClusterID}
                                 style={{width: "200px"}}
                                 onChange={(event) => this.setParameterProperty("cloudClusterID", event.target.value)}/>
                    </div>
                  </div>
                  <div className="row">
                    <div className="col-md-6">
                      <TextInput labelText="client ID"
                                 readonly="true"
                                 value={this.state.parameters.cloudClientID}
                                 style={{width: "300px"}}
                                 onChange={(event) => this.setParameterProperty("cloudClientID", event.target.value)}/>
                    </div>
                  </div>
                  <div className="row">
                    <div className="col-md-6">
                      <TextInput labelText="Client secret"
                                 readonly="true"
                                 style={{width: "300px"}}
                                 value={this.state.parameters.cloudClientSecret}
                                 onChange={(event) => this.setParameterProperty("cloudClientSecret", event.target.value)}/>
                    </div>
                  </div>
                </div>

              </div>
            </div>
          </div>



          <div className="col-md-6">

            <div className="card" style={{width: "25rem;"}}>
              <div className="card-header" style={{backgroundColor: "rgba(0,0,0,.03)"}}>Database</div>
              <div className="card-body">
                <div className="row">
                  <div className="col-md-6">
                    <TextInput labelText="Product"
                               readonly="true"
                               value={this.state.parameters.datasourceProductName}
                               style={{width: "300px"}}/>
                  </div>
                </div>
                <div className="row">
                  <div className="col-md-6">
                    <TextInput labelText="Datasource"
                               readonly="true"
                               value={this.state.parameters.datasourceUrl}
                               style={{width: "300px"}}
                               onChange={(event) => this.setParameterProperty("datasourceUrl", event.target.value)}/>
                  </div>
                </div>
                <div className="row">
                  <div className="col-md-6">
                    <TextInput labelText="User Name"
                               readonly="true"
                               value={this.state.parameters.datasourceUserName}
                               style={{width: "300px"}}
                               onChange={(event) => this.setParameterProperty("datasourceUserName", event.target.value)}/>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>





        <div className="row" style={{marginTop: "10px"}}>
          <div className="col-md-6">
            <div className="card" style={{width: "25rem;"}}>
              <div className="card-header" style={{backgroundColor: "rgba(0,0,0,.03)"}}>Worker</div>
              <div className="card-body">
                <div className="row">
                  <div className="col-md-12">
                    <NumberInput label="Max job actives"
                                 readonly="true"
                                 min="1"
                                 size="sm"
                                 value={this.state.parameters.maxJobsActive}
                                 style={{width: "100px"}}
                                 onChange={(event) => this.setParameterProperty("maxJobsActive", event.target.value)}/>
                  </div>
                </div>
                <div className="row">
                  <div className="col-md-12">
                    <NumberInput label="Threads"
                                 readonly="true"
                                 min="1"
                                 size="sm"
                                 value={this.state.parameters.nbThreads}
                                 style={{width: "100px"}}
                                 onChange={(event) => this.setParameterProperty("nbThreads", event.target.value)}/>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>


      </div>
    )

  }

  getZssZeebeConnection(value) {
    console.log("getZssZeebeConnection : value="+value+"] statePara["+this.state.parameters.zeebekindconnection+"]")
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