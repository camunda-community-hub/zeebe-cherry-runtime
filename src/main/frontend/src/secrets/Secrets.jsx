// -----------------------------------------------------------
//
// Parameters
//
// List of all runners available
//
// -----------------------------------------------------------

import React from 'react';
import RestCallService from "../services/RestCallService";
import {Button, TextInput, Toggle} from "carbon-components-react";
import {ArrowRepeat, ExclamationTriangle} from "react-bootstrap-icons";
import ControllerPage from "../component/ControllerPage";


class Secrets extends React.Component {


  constructor(_props) {
    super();
    this.state = {
      secrets: [],
      display: {loading: false}
    };
  }

  componentDidMount(prevProps) {
    this.refreshListSecrets();
  }

  render() {
    return (
      <div className={"container"}>
        <div className="row" style={{width: "100%"}}>
          <div className="col-md-10">
            <h1 className="title">Secrets</h1>
          </div>
          <div className="col-md-2">
            <Button className="btn btn-success btn-sm"
                    onClick={() => this.refreshListSecrets()}
                    disabled={this.state.display.loading}>
              <ArrowRepeat/> Refresh
            </Button>
          </div>

          <div className="row" style={{width: "100%"}}>
            <div className="col-md-12">
              <ControllerPage errorMessage={this.state.status} loading={this.state.display.loading}/>
            </div>
          </div>


          <div className="row" style={{width: "100%"}}>
            <div className="col-md-12">

              <table id="runnersTable" className="table is-hoverable is-fullwidth">
                <thead>
                <tr>
                  <th>Name</th>
                  <th>Secret</th>
                  <th>Value</th>
                  <th></th>
                </tr>
                </thead>
                <tbody>
                {this.state.secrets ? this.state.secrets.map((secret, _index) =>
                  <tr style={this.getStyleRow(secret)}>
                    <td style={{verticalAlign: "top"}}>
                      <TextInput disabled={this.state.display.loading}
                                 value={secret.name}
                                 onChange={(event) => {
                                   secret.name = event.target.value;
                                   secret.somethingChange = true;
                                   this.forceUpdate()
                                 }}/><br/>
                      {secret.error && <div className="alert alert-danger"><ExclamationTriangle/>{secret.error}</div>}
                    </td>
                    <td>
                      <Toggle
                        toggled={secret.issecret}
                        onToggle={(event) => {
                          this.changeSecret(secret, event);
                        }}
                        labelA="Visible"
                        labelB="Secret"/>


                    </td>
                    <td style={{verticalAlign: "top"}}>
                      <TextInput disabled={this.state.display.loading || secret.issecret}
                                 value={secret.issecret ? "" : secret.value}
                                 onChange={(event) => {
                                   secret.value = event.target.value;
                                   secret.somethingChange = true;
                                   this.forceUpdate()
                                 }}/>
                    </td>
                    <td>
                      <Button className="btn btn-primary btn-sm"
                              onClick={() => this.updateSecret(secret)}
                              style={{marginRight: "10px"}}
                              disabled={!secret.somethingChange}
                      >
                        Update
                      </Button>

                      <Button className="btn btn-danger btn-sm"
                              onClick={() => this.deleteSecret(secret)}>
                        Delete
                      </Button>

                    </td>
                  </tr>
                ) : <div/>}
                </tbody>
              </table>
              <Button className="btn btn-success btn-sm"
                      onClick={() => this.addSecret()}>
                Add a new entry
              </Button>
              <div className="alert alert-info" style={{margin: "10px 10px 10px 10px"}}>
                When the toggle Secret is on, the value is stored on the server and is not accessible on the portal. You
                can change it:
                <li> set the toggle to Visible,</li>
                <li> change the value</li>
                <li> set the Toggle again to Secret before</li>
                <li> updating the value</li>
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  getStyleRow(secret) {
    return {};
  }

  refreshListSecrets() {
    let uri = 'cherry/api/secretenv/list?type=SECRET';
    console.log("Secrets.refreshListSecrets http[" + uri + "]");

    this.setDisplayProperty("loading", true);
    this.setState({status: ""});
    var restCallService = RestCallService.getInstance();
    restCallService.getJson(uri, this, this.refreshListSecretsCallback);
  }

  refreshListSecretsCallback(httpPayload) {
    this.setDisplayProperty("loading", false);
    if (httpPayload.isError()) {
      console.log("Store.refreshListConnectorsCallback: error " + httpPayload.getError());
      this.setState({status: httpPayload.getError()});
    } else {
      let listSecrets = httpPayload.getData();
      for (let secretIndex in listSecrets) {
        listSecrets[secretIndex].oldname = listSecrets[secretIndex].name;
        listSecrets[secretIndex].somethingChange = false;
      }

      this.setState({secrets: listSecrets});

    }
  }

  /**
   * Add a new line
   */
  addSecret() {
    let listSecrets = this.state.secrets;
    if (!listSecrets)
      listSecrets = [];
    listSecrets.push({issecret: false, somethingChange: true});
    this.setState({secrets: listSecrets});
  }

  changeSecret(secret, value) {
    if (value === true) {
      secret.issecret = true;
    } else {
      secret.issecret = false;
    }
    secret.somethingChange = true;
    this.updateLocalSecret(secret);

  }

  updateLocalSecret(secret) {
    let listSecrets = this.state.secrets;
    for (let secretIndex in listSecrets) {
      if (listSecrets[secretIndex] === secret)
        listSecrets[secretIndex] = secret;
    }
    this.setState({secrets: listSecrets});

  }

  updateSecret(secret) {
    let uri = 'cherry/api/secretenv/update?type=SECRET'
      + "&name=" + secret.name
      + "&issecret=" + secret.issecret;
    if (secret.value)
      uri = uri + "&value=" + secret.value;
    if (secret.id)
      uri = uri + '&id=' + secret.id;

    secret.error = ""; // clear error
    console.log("Secrets.update http[" + uri + "]");

    this.setDisplayProperty("loading", true);
    this.setState({status: ""});
    let restCallService = RestCallService.getInstance();
    this.saveSecret = secret;
    restCallService.postJson(uri, {}, this, this.updateSecretCallback);
  }

  updateSecretCallback(httpPayload) {
    if (httpPayload.isError()) {
      console.log("Store.refreshListConnectorsCallback: error " + httpPayload.getError());
      this.setState({status: httpPayload.getError()});
    } else {
      debugger;
      let secretInfo = httpPayload.getData();
      this.saveSecret.id = secretInfo.id;
      this.saveSecret.error = secretInfo.error;
      // refresh: the display Property will do the job
    }
    this.setDisplayProperty("loading", false);

  }


  deleteSecret(secret) {
    if (!secret.id) {
      // this is a new entry, just delete it
      let listSecrets = this.state.secrets;
      listSecrets.delete(secret)
      this.setState({secrets: listSecrets});
      return;
    }
    let uri = 'cherry/api/secretenv/delete?id=' + secret.id;
    console.log("Secrets.delete http[" + uri + "]");

    this.setDisplayProperty("loading", true);
    this.setState({status: ""});
    var restCallService = RestCallService.getInstance();
    restCallService.postJson(uri, {}, this, this.deleteSecretCallback);
  }

  deleteSecretCallback(httpPayload) {
    this.setDisplayProperty("loading", false);
    if (httpPayload.isError()) {
      console.log("Store.refreshListConnectorsCallback: error " + httpPayload.getError());
      this.setState({status: httpPayload.getError()});
    } else {
      debugger;
      let secretInfo = httpPayload.getData();
      // search it to update it
      let listSecrets = this.state.secrets;
      for (let i = 0; i < listSecrets.length; i++) {
        if (listSecrets[i].id === secretInfo.id) {
          listSecrets.splice(i, 1);
          break;
        }
      }
      this.setState({secrets: listSecrets});

    }
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
}

export default Secrets;