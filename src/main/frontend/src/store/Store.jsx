// -----------------------------------------------------------
//
// Parameters
//
// List of all runners available
//
// -----------------------------------------------------------

import React from 'react';
import {Button, Tag} from "carbon-components-react";
import {ArrowRepeat, CloudDownloadFill, ConeStriped} from "react-bootstrap-icons";

import RestCallService from "../services/RestCallService";
import ControllerPage from "../component/ControllerPage";

class Store extends React.Component {


  constructor(_props) {
    super();
    this.state = {
      connectors: [],
      display: {loading: false}
    };
  }

  componentDidMount(prevProps) {
    this.refreshListConnectors();

  }

  render() {
    return (
      <div class={"container"}>
        <div className="row" style={{width: "100%"}}>
          <div className="col-md-10">
            <h1 className="title">Store</h1>
          </div>
          <div className="col-md-2">
            <Button className="btn btn-success btn-sm"
                    onClick={() => this.refreshListConnectors()}
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
                  <th>Connector</th>
                  <th>Release</th>
                  <th>Status</th>
                  <th>Operation</th>
                </tr>
                </thead>
                <tbody>
                {this.state.connectors ? this.state.connectors.map((connectorStore, _index) =>
                  <tr style={this.getStyleRow(connectorStore)}>
                    <td>
                      {connectorStore.name}
                    </td>
                    <td>
                      {connectorStore.release}
                    </td>
                    <td>
                      {connectorStore.status === "NEW" && <Tag type="green" title="New">New</Tag>}
                      {connectorStore.status === "OLD" &&
                        <Tag type="purple" title="New">New version {connectorStore.storerelease}</Tag>}
                    </td>
                    <td>
                      {(connectorStore.status === "NEW" || connectorStore.status === "OLD") &&
                        <Button className="btn btn-primary btn-sm"
                                onClick={() => this.downloadConnector(connectorStore)}>
                          <ConeStriped style={{color: "red"}}/>
                          <CloudDownloadFill/> Download
                        </Button>
                      }
                    </td>
                  </tr>
                ) : <div/>}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    )
  }

  getStyleRow(connectorMarket) {
    return {};
  }

  refreshListConnectors(period, orderBy) {
    let uri = 'cherry/api/store/list?';
    console.log("Store.refreshListConnectors http[" + uri + "]");

    this.setDisplayProperty("loading", true);
    this.setState({status: ""});
    let restCallService = RestCallService.getInstance();
    restCallService.getJson(uri, this, this.refreshListConnectorsCallback);
  }

  refreshListConnectorsCallback(httpPayload) {
    this.setDisplayProperty("loading", false);
    if (httpPayload.isError()) {
      console.log("Store.refreshListConnectorsCallback: error " + httpPayload.getError());
      this.setState({status: "ControllerPage " + httpPayload.getError()});
    } else {

      this.setState({connectors: httpPayload.getData()});

    }
  }

  downloadConnector(connector) {
    let uri = 'cherry/api/store/download?name=' + connector.name;

    this.setDisplayProperty("loading", true);
    this.setState({status: ""});
    let restCallService = RestCallService.getInstance();
    restCallService.getJson(uri, this, this.downloadConnectorsCallback);
  }

  downloadConnectorsCallback(httpPayload) {
    this.setDisplayProperty("loading", false);
    if (httpPayload.isError()) {
      console.log("Store.refreshListConnectorsCallback: error " + httpPayload.getError());
      this.setState({status: httpPayload.getError()});
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

export default Store;