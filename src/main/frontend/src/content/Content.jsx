// -----------------------------------------------------------
//
// Content
//
// List of all jar file uploaded
//
// -----------------------------------------------------------

import React, {createRef} from 'react';
import {Button, FileUploader} from "carbon-components-react";
import {ArrowRepeat} from "react-bootstrap-icons";
import ControllerPage from "../component/ControllerPage";
import RestCallService from "../services/RestCallService";

class Content extends React.Component {


  constructor(_props) {
    super();
    this.fileUploaderRef = createRef();

    this.state = {
      content: [],
      files: [],
      display: {loading: false},
      status: ""
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
                    onClick={() => {
                      this.refreshStatusOnPage();
                      this.refreshListContent()
                    }}
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
                <th>Loaded</th>
                <th>Log</th>
                <th></th>

              </tr>
              </thead>
              <tbody>
              {this.state.content ? this.state.content.map((content, _index) =>
                <tr style={this.getStyleRow(content)}>
                  <td style={{verticalAlign: "top"}}>
                    {content.name}
                  </td>
                  <td style={{verticalAlign: "top"}}>
                    {content.usedby.map((usedby, _indexcontent) =>
                      <div>{usedby.name} {usedby.collectionName} <br/>
                        {usedby.activeRunner &&
                          <button className="start-runner button is-selected is-primary">Started</button>
                        }
                        {!usedby.activeRunner &&
                          <button className="stop-runner button is-selected is-danger">Stopped</button>
                        }
                      </div>
                    )
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
                            disabled={this.state.display.loading}
                    >
                      Delete
                    </Button>
                  </td>
                </tr>
              ) : <div/>}
              </tbody>
            </table>

          </div>
        </div>
        <div className="row" style={{width: "100%"}}>
          <div className="col-md-12">
            <FileUploader
              ref={this.fileUploaderRef}
              labelTitle="Upload JAR files"
              labelDescription="Only .jar file"
              buttonLabel="Add files"
              filenameStatus="edit"
              accept={['.jar']}
              onChange={(event) => this.handleFileChange(event)}
              multiple
              iconDescription="Clear file"
              disabled={this.state.display.loading || this.state.files.size === 0}
            />
            <Button onClick={() => this.loadJar()} disabled={this.state.display.loading}>Upload</Button>
            <br/>
            Upload a Connector Jar directly from your disk to Cherry. It will be analysed and all workers/connectors
            detected started.

            {this.state.statusUploadFailed && <div className="alert alert-danger" style={{
              margin: "10px 10px 10px" +
                " 10px"
            }}>
              {this.state.statusUploadFailed}
            </div>
            }
            {this.state.statusUploadSuccess && <div className="alert alert-success" style={{
              margin: "10px 10px 10px" +
                " 10px"
            }}>
              {this.state.statusUploadSuccess}
            </div>}
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
      console.log("Content.refreshListContentCallback: error: " + httpPayload.getError());
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

  deleteStorageEntityId(storageentityid) {
    this.refreshStatusOnPage();
    console.log("Content.deleteStorageEntityId" + storageentityid);
    const userConfirmed = window.confirm("Are you sure you want to delete this Jar?");
    if (userConfirmed) {
      this.setDisplayProperty("loading", true);
      this.setState({labelBtnStop: "Deleting...", status: ""});
      var restCallService = RestCallService.getInstance();
      restCallService.putJson('cherry/api/content/delete?storageentityid=' + storageentityid, {}, this, this.operationDeleteCallback);
    }
  }

  operationDeleteCallback(httpResponse) {
    this.setDisplayProperty("loading", false);

    if (httpResponse.isError()) {
      console.log("deleteStorageEntityId.operationDeleteCallback: error " + httpResponse.getError());
      this.setState({status: httpResponse.getError()});
    } else {
    }
    this.refreshListContent();
  }

  handleFileChange(event) {
    this.refreshStatusOnPage();
    const fileList = event.target.files;
    this.setState({files: fileList}); // Use spread operator to create a new array
  };


  loadJar(event) {
    console.log("Load Jar ", this.state.files);
    this.refreshStatusOnPage();
    let restCallService = RestCallService.getInstance();

    const formData = new FormData();
    Array.from(this.state.files).forEach((file, index) => {
      formData.append(`File`, file);
    });
    /* formData.append("File", this.state.files[0]); */
    this.setDisplayProperty("loading", true);

    restCallService.postUpload('cherry/api/content/add?', formData, this, this.operationUploadJarCallback);


    // dispatch(connectorService.uploadJar(event.target.files[0]));
  }

  operationUploadJarCallback(httpResponse) {
    this.setDisplayProperty("loading", false);

    if (httpResponse.isError()) {
      console.log("operationUploadJar.operationDeleteCallback: error " + httpResponse.getError());
      this.setState({statusUploadFailed: httpResponse.getError()});
    } else {
      // Clear the file input field using JavaScript
      if (this.fileUploaderRef.current) {
        this.fileUploaderRef.current.clearFiles();
      }
      this.setState({'files': [], statusUploadSuccess: 'Jar uploaded with success'});
    }
    this.refreshListContent();
  }

  refreshStatusOnPage() {
    this.setState({statusUploadFailed: '', statusUploadSuccess: '', status: ''});
  }
}

export default Content;