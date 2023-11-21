// -----------------------------------------------------------
//
// Content
//
// List of all jar file uploaded
//
// -----------------------------------------------------------

import React from 'react';
import {Button} from "carbon-components-react";
import {ArrowRepeat, ConeStriped} from "react-bootstrap-icons";
import ControllerPage from "../component/ControllerPage";
import RestCallService from "../services/RestCallService";

class Content extends React.Component {


  constructor(_props) {
    super();
    this.state = {
      content: [],
      display: {loading: false},
      status:""
    };
  }

  componentDidMount() {
    this.refreshListContent();
  }

  /*           {JSON.stringify(this.state.runners, null, 2) } */
  render() {
    return (
      <div className={"container"}>
        <div className="row" style={{width: "100%"}}>
          <div className="col-md-10">
            <h1 className="title">Content</h1>
          </div>
          <div className="col-md-2">
            <Button className="btn btn-success btn-sm"
                    onClick={() => this.refreshListContent()}
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


        <div className="row" style={{width: "100%"}}>
          <div className="col-md-12">

            <table id="runnersTable" className="table is-hoverable is-fullwidth">
              <thead>
              <tr>
                <th>Name</th>
                <th>Used by</th>
                <th>Loaded time</th>
                <th>Log</th>
                <th> </th>

              </tr>
              </thead>
              <tbody>
              {this.state.content ? this.state.content.map((content, _index) =>
                <tr style={this.getStyleRow(content)}>
                  <td style={{verticalAlign: "top"}}>
                    {content.name}
                  </td>
                  <td>
                  {content.usedby.map((usedby, _indexcontent) =>
                    <div>{usedby.name} {usedby.collection} <br/></div>)
                  }
                  </td>
                  <td style={{verticalAlign: "top"}}>
                    {content.loadedtime}
                  </td>
                  <td style={{verticalAlign: "top"}}>
                    {content.loadlog}
                  </td>
                  <td>
                    <Button className="btn btn-danger btn-sm"
                            onClick={() => this.deleteStorageEntityId(content.storageentityid)}
                            style={{marginRight: "10px"}}
                            disabled={this.state.display.loading }
                    >
                      <ConeStriped style={{color: "red"}}/>
                      Delete
                    </Button>
                  </td>
                </tr>
              ) : <div/>}
              </tbody>
            </table>

          </div>
        </div>
      </div>
    )
  }


  getStyleRow(_content) {
    return {};
  }
  refreshListContent() {
    let uri = 'cherry/api/content/list?';
    console.log("Content.refreshListContent http[" + uri + "]");

    this.setDisplayProperty("loading", true);
    this.setState({status: ""});
    var restCallService = RestCallService.getInstance();
    restCallService.getJson(uri, this, this.refreshListContentCallback);
  }

  refreshListContentCallback(httpPayload) {
    this.setDisplayProperty("loading", false);
    if (httpPayload.isError()) {
      console.log("Content.refreshListContentCallback: error " + httpPayload.getError());
      this.setState({status: httpPayload.getError()});
    } else {
      this.setState({content: httpPayload.getData()});
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

  deleteStorageEntityId( storageentityid) {
    console.log("Content.deleteStorageEntityId"+storageentityid);
    const userConfirmed = window.confirm("Are you sure you want to delete this Jar?");
    if (userConfirmed) {
      this.setState({labelBtnStop: "Deleting...", status: ""});
      var restCallService = RestCallService.getInstance();
      restCallService.putJson('cherry/api/content/delete?storageentityid=' + storageentityid, {}, this, this.operationDeleteCallback);
    }
  }

  operationDeleteCallback(httpResponse) {
    if (httpResponse.isError()) {
      console.log("deleteStorageEntityId.operationDeleteCallback: error " + httpResponse.getError());
      this.setState({status: httpResponse.getError()});
    } else {
    }
    this.refreshListContent();
  }
}

export default Content;