// -----------------------------------------------------------
//
// Parameters
//
// List of all runners available
//
// -----------------------------------------------------------

import React from 'react';
import {Button, Tag} from "carbon-components-react";
import {ArrowRepeat, ChevronDown, ChevronUp, CloudDownloadFill, ConeStriped} from "react-bootstrap-icons";

import RestCallService from "../services/RestCallService";
import ControllerPage from "../component/ControllerPage";

class Store extends React.Component {

    constructor(_props) {
        super();
        this.state = {
            connectors: [],
            stores: [],
            display: {loading: false},
            expandedRows: {}
        };
    }

    componentDidMount(prevProps) {
        this.loadStores();
    }

    loadStores() {
        let restCallService = RestCallService.getInstance();
        restCallService.getJson('cherry/api/store/list?', this, this.loadStoresCallback);
    }

    loadStoresCallback(httpPayload) {
        if (httpPayload.isError()) {
            this.setState({status: httpPayload.getError()});
        } else {
            const stores = httpPayload.getData().map(r => ({...r, selected: true}));
            this.setState({stores}, () => this.refreshListConnectors());
        }
    }

    toggleRow(index) {
        this.setState(prev => ({
            expandedRows: {...prev.expandedRows, [index]: !prev.expandedRows[index]}
        }));
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

                    <div className="col-md-6">
                        <div className="btn-group" role="group" style={{padding: "10px 10px 10px 10px"}}>
                            {this.state.stores.map((store, _index) =>
                                <button key={store.name}
                                        className={this.getButtonClass(store.selected)}
                                        style={{marginLeft: "10px", fontSize: "10px"}}
                                        disabled={this.state.display.loading}
                                        onClick={() => this.setStoreFilter(store.name)}>
                                    {store.name}
                                </button>
                            )}
                        </div>
                    </div>

                    <div className="row" style={{width: "100%"}}>
                        <div className="col-md-12">
                            <table id="runnersTable" className="table is-hoverable is-fullwidth">
                                <thead>
                                <tr>
                                    <th></th>
                                    <th>Icon</th>
                                    <th>Connector</th>
                                    <th>Store</th>
                                    <th>Release</th>
                                    <th>Status</th>
                                    <th>Operation</th>
                                    <th></th>
                                </tr>
                                </thead>
                                <tbody>
                                {this.state.connectors ? this.state.connectors.map((connectorStore, index) => (
                                    <React.Fragment key={index}>
                                        <tr style={this.getStyleRow(connectorStore)}>
                                            <td style={{width: "30px", cursor: "pointer"}}
                                                onClick={() => this.toggleRow(index)}>
                                                {this.state.expandedRows[index]
                                                    ? <ChevronDown/>
                                                    : <ChevronUp/>}
                                            </td>
                                            <td style={{width: "32px"}}>
                                                {connectorStore.icon &&
                                                    <img src={connectorStore.icon} alt=""
                                                         style={{width: "24px", height: "24px"}}/>}
                                            </td>
                                            <td>{connectorStore.name}</td>
                                            <td>
                                                <Tag type="blue">{connectorStore.store}</Tag>
                                            </td>
                                            <td>
                                               {connectorStore.currentrelease} ({connectorStore.storerelease})
                                            </td>
                                            <td>
                                                {connectorStore.status === "NEW" &&
                                                    <Tag type="green" title="New">New</Tag>}
                                                {connectorStore.status === "OLD" &&
                                                    <Tag type="purple" title="Old">New
                                                        version {connectorStore.storerelease}</Tag>}
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
                                        {this.state.expandedRows[index] && (
                                            <tr>
                                                <td colSpan="8">
                                                    <div className="card" style={{margin: "8px 0 8px 60px"}}>
                                                        <div className="card-header"><strong>Information</strong></div>
                                                        <div className="card-body" style={{fontSize: "12px"}}>
                                                            {connectorStore.icon && (
                                                                <div style={{display: "flex", alignItems: "center", marginBottom: "6px"}}>
                                                                    <img src={connectorStore.icon} alt=""
                                                                         style={{width: "48px", height: "48px", marginRight: "12px"}}/>
                                                                    <strong style={{fontSize: "14px"}}>{connectorStore.name}</strong>
                                                                </div>
                                                            )}
                                                            <div><strong>Connector Type:</strong> {connectorStore.connectorType}</div>
                                                            <div><strong>Description:</strong> {connectorStore.description}</div>
                                                            <div><strong>Store:</strong> {connectorStore.store}</div>
                                                            <div><strong>Store Release:</strong> {connectorStore.storerelease}</div>
                                                            <div><strong>Documentation:</strong>&nbsp;
                                                                {connectorStore.documentationRef
                                                                    ? <a href={connectorStore.documentationRef} target="_blank" rel="noreferrer">{connectorStore.documentationRef}</a>
                                                                    : "-"}
                                                            </div>
                                                            <div><strong>GitHub Repo:</strong> {connectorStore.githubRepoName}</div>
                                                            <div><strong>GitHub Path:</strong> {connectorStore.githubRepoPath}</div>
                                                            <div><strong>Exploration Status:</strong> {connectorStore.explorationStatus}</div>
                                                            <div><strong>Element Template URL:</strong>&nbsp;
                                                                {connectorStore.urlElementTemplate
                                                                    ? <a href={connectorStore.urlElementTemplate} target="_blank" rel="noreferrer">{connectorStore.urlElementTemplate}</a>
                                                                    : "-"}
                                                            </div>
                                                            <div><strong>Jar File URL:</strong>&nbsp;
                                                                {connectorStore.urlJarFile
                                                                    ? <a href={connectorStore.urlJarFile} target="_blank" rel="noreferrer">{connectorStore.urlJarFile}</a>
                                                                    : "-"}
                                                            </div>
                                                            <div><strong>Maven URL:</strong>&nbsp;
                                                                {connectorStore.urlMaven
                                                                    ? <a href={connectorStore.urlMaven} target="_blank" rel="noreferrer">{connectorStore.urlMaven}</a>
                                                                    : "-"}
                                                            </div>
                                                            <div><strong>Has Implementation:</strong> {String(connectorStore.hasImplementation)}</div>
                                                        </div>
                                                    </div>
                                                </td>
                                            </tr>
                                        )}
                                    </React.Fragment>
                                )) : <tr><td colSpan="9"/></tr>}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    getStyleRow(connectorMarket) {
        return {};
    }

    refreshListConnectors() {
        const selectedRepos = this.state.stores
            .filter(r => r.selected)
            .map(r => r.name);
        const repoParams = selectedRepos.map(name => `stores=${encodeURIComponent(name)}`).join('&');
        let uri = 'cherry/api/store/connectors/list?' + repoParams;
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
            this.setState({status: httpPayload.getError()});
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

    getButtonClass(active) {
        if (active)
            return "btn btn-primary btn-sm";
        return "btn btn-outline-primary btn-sm";
    }

    setStoreFilter(storeName) {
        let stores = this.state.stores.map(r =>
            r.name === storeName ? {...r, selected: !r.selected} : r
        );
        this.setState({stores}, () => this.refreshListConnectors());
    }

    setDisplayProperty(propertyName, propertyValue) {
        let displayObject = this.state.display;
        displayObject[propertyName] = propertyValue;
        this.setState({display: displayObject});
    }
}

export default Store;
