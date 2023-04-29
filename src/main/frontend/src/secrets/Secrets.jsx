// -----------------------------------------------------------
//
// Parameters
//
// List of all runners available
//
// -----------------------------------------------------------

import React from 'react';
import RestCallService from "../services/RestCallService";


class Secrets extends React.Component {


  constructor(_props) {
    super();
    this.state = {
      secrets: [],
      display: {loading: false}
    };
  }

  componentDidMount(prevProps) {

  }

  render() {
    return (<div class={"container"}><h1>Secrets</h1>

        <table width="100%">
          {this.state.secrets ? this.state.secrets.map((secret, _index) =>
            <tr style={this.getStyleRow(secret)}>
              <td>
                {secret.name}
              </td>

            </tr>
          ) : <div/>}
        </table>
      </div>
    )
  }

  getStyleRow(secret) {
    return {};
  }

  refreshListSecrets(period, orderBy) {
    let uri = 'cherry/api/secret/list?';
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
      this.setState({status: "Error"});
    } else {

      this.setState({connectors: httpPayload.getData()});

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