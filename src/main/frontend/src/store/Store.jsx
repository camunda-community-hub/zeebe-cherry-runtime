// -----------------------------------------------------------
//
// Parameters
//
// List of all runners available
//
// -----------------------------------------------------------

import React from 'react';
import {Button, Select, Tag, TextInput} from "carbon-components-react";
import {
    ArrowRepeat,
    ChevronDown,
    ChevronRight,
    CloudDownloadFill,
    FileEarmarkCode,
    FileEarmarkText,
    FileEarmarkZip
} from "react-bootstrap-icons";

import RestCallService from "../services/RestCallService";
import ControllerPage from "../component/ControllerPage";
import DownloadMessage from "../HeaderMessage/DownloadMessage";

class Store extends React.Component {

    constructor(_props) {
        super();
        const savedDisplay = (() => {
            try {
                return JSON.parse(localStorage.getItem("store_display") || "{}");
            } catch {
                return {};
            }
        })();
        this.state = {
            connectors: [],
            stores: [],
            display: {
                loading: false,
                orderBy: savedDisplay.orderBy || "nameAsc",
                statusFilter: savedDisplay.statusFilter || "ALL",
                filterSearch: savedDisplay.filterSearch || ""
            },
            expandedRows: {}
        };
        this.setOrderBy = this.setOrderBy.bind(this);
    }

    componentDidMount(prevProps) {
        this.loadStores();
        this.refreshListConnectors();
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
            <div className={"container"}>
                <div className="row" style={{width: "100%", alignItems: "center"}}>
                    <div className="col-md-auto">
                        <h1 className="title">Store</h1>
                    </div>
                    <div className="col-md-auto" style={{marginLeft: "auto"}}>
                        <Button className="btn btn-warning btn-sm"
                                onClick={() => this.exploreAgain()}
                                disabled={this.state.display.loading}>
                            <ArrowRepeat/> Explore again
                        </Button>
                    </div>
                </div>

                <div className="row" style={{width: "100%"}}>
                    <div className="col-md-12">
                        <ControllerPage errorMessage={this.state.status} loading={this.state.display.loading}/>
                    </div>
                </div>

                <div className="row" style={{width: "100%", alignItems: "flex-end"}}>
                    {/* Store filter buttons */}
                    <div className="col-md-auto">
                        <div className="btn-group" role="group" style={{padding: "10px 10px 10px 10px"}}>
                            {this.state.stores.map((store) =>
                                <button key={store.name}
                                        className={this.getButtonClass(store.selected)}
                                        style={{fontSize: "10px", height: "40px"}}
                                        disabled={this.state.display.loading}
                                        onClick={() => this.setStoreFilter(store.name)}>
                                    {store.name}
                                </button>
                            )}
                        </div>
                    </div>

                    {/* Status filter buttons */}
                    <div className="col-md-auto">
                        <div className="btn-group" role="group" style={{padding: "10px 10px 10px 10px"}}>
                            <button className={this.getButtonClass(this.state.display.statusFilter === "ALL")}
                                    style={{marginLeft: "10px", fontSize: "10px", height: "40px"}}
                                    disabled={this.state.display.loading}
                                    onClick={() => this.setStatusFilter("ALL")}>
                                All
                            </button>
                            <button className={this.getButtonClass(this.state.display.statusFilter === "INSTALLABLE")}
                                    style={{fontSize: "10px", height: "40px"}}
                                    disabled={this.state.display.loading}
                                    onClick={() => this.setStatusFilter("INSTALLABLE")}>
                                Installable
                            </button>
                            <button className={this.getButtonClass(this.state.display.statusFilter === "NOT-INSTALLED")}
                                    style={{fontSize: "10px", height: "40px"}}
                                    disabled={this.state.display.loading}
                                    onClick={() => this.setStatusFilter("NOT-INSTALLED")}>
                                Not installed
                            </button>
                            <button className={this.getButtonClass(this.state.display.statusFilter === "OLD")}
                                    style={{fontSize: "10px", height: "40px"}}
                                    disabled={this.state.display.loading}
                                    onClick={() => this.setStatusFilter("OLD")}>
                                Old
                            </button>
                        </div>
                    </div>

                    {/* Text filter */}
                    <div className="col-md-2">
                        <TextInput labelText="Filter"
                                   disabled={this.state.display.loading}
                                   value={this.state.display.filterSearch}
                                   style={{width: "120px"}}
                                   onChange={(event) => this.setSearchFilter(event.target.value)}/>
                    </div>

                    {/* Order by */}
                    <div className="col-md-2">
                        <Select value={this.state.display.orderBy}
                                labelText="Order by"
                                disabled={this.state.display.loading}
                                onChange={(event) => this.setOrderBy(event.target.value)}>
                            <option value="nameAsc">Name (asc)</option>
                            <option value="nameDesc">Name (desc)</option>
                            <option value="marketplaceAsc">Marketplace (asc)</option>
                            <option value="marketplaceDesc">Marketplace (desc)</option>
                        </Select>
                    </div>

                    {/* Refresh button */}
                    <div className="col-md-1">
                        <Button className="btn btn-success btn-sm"
                                onClick={() => this.refreshListConnectors()}
                                disabled={this.state.display.loading}>
                            <ArrowRepeat/> Refresh
                        </Button>
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
                            {this.state.connectors ? this.state.connectors.map((connectorDefinition, index) => (
                                <React.Fragment key={index}>
                                    <tr style={this.getStyleRow(connectorDefinition)}>
                                        <td style={{width: "30px", cursor: "pointer"}}
                                            onClick={() => this.toggleRow(index)}>
                                            {this.state.expandedRows[index]
                                                ? <ChevronDown/>
                                                : <ChevronRight/>}
                                        </td>
                                        <td style={{width: "32px"}}>
                                            {connectorDefinition.icon &&
                                                <img src={connectorDefinition.icon} alt=""
                                                     style={{width: "24px", height: "24px"}}/>}
                                        </td>
                                        <td>
                                            {connectorDefinition.name}
                                            {connectorDefinition.documentationRef && (
                                                <a href={connectorDefinition.documentationRef} target="_blank"
                                                   rel="noreferrer" title="Documentation" style={{marginLeft: "6px"}}>
                                                    <FileEarmarkText/>
                                                </a>
                                            )}
                                            {connectorDefinition.urlElementTemplate && (
                                                <a href={`cherry/api/store/connectors/downloadElementTemplate?store=${encodeURIComponent(connectorDefinition.store)}&connectorName=${encodeURIComponent(connectorDefinition.name)}&release=${encodeURIComponent(connectorDefinition.storerelease)}`}
                                                   title="Download Element Template" style={{marginLeft: "6px"}}>
                                                    <FileEarmarkCode/>
                                                </a>
                                            )}
                                            {connectorDefinition.urlJarFile && (
                                                <a href={`cherry/api/store/connectors/downloadJarFile?store=${encodeURIComponent(connectorDefinition.store)}&connectorName=${encodeURIComponent(connectorDefinition.name)}&release=${encodeURIComponent(connectorDefinition.storerelease)}`}
                                                   title="Download JAR" style={{marginLeft: "6px"}}>
                                                    <FileEarmarkZip/>
                                                </a>
                                            )}
                                        </td>
                                        <td>
                                            <Tag type="blue">{connectorDefinition.store}</Tag>
                                        </td>
                                        <td>
                                            {connectorDefinition.currentrelease} ({connectorDefinition.storerelease})
                                        </td>
                                        <td>
                                            {connectorDefinition.explorationStatus === "FAILED" &&
                                                <Tag type="red" title="Exploration failed">Exploration Failed</Tag>}
                                            {connectorDefinition.explorationStatus !== "FAILED" && connectorDefinition.status === "NOT-INSTALLED" &&
                                                <Tag type="purple" title="Not installed">Not installed</Tag>}
                                            {connectorDefinition.explorationStatus !== "FAILED" && connectorDefinition.status === "NO_IMPLEMENTATION" &&
                                                <Tag type="purple" title="No implementation">No implementation</Tag>}
                                            {connectorDefinition.explorationStatus !== "FAILED" && connectorDefinition.status === "PARENT-NOT-INSTALLED" &&
                                                <div>
                                                    <Tag type="purple" title="Parent Not installed">Parent Not
                                                        installed</Tag>
                                                    <div style={{fontSize: "10px"}}>Parent connector
                                                        type: {connectorDefinition.connectorType}</div>
                                                </div>}
                                            {connectorDefinition.explorationStatus !== "FAILED" && connectorDefinition.status === "UPDATED" &&
                                                <Tag type="blue" title="Up to date">Up to date</Tag>}
                                            {connectorDefinition.explorationStatus !== "FAILED" && connectorDefinition.status === "OLD" &&
                                                <Tag type="warm-gray" title="Old">New
                                                    version {connectorDefinition.storerelease}</Tag>}
                                            {connectorDefinition.explorationStatus !== "FAILED" && connectorDefinition.status === "NO-RELEASE" &&
                                                <Tag type="blue" title="No release">No release</Tag>}
                                            {connectorDefinition.status === "IN-PROGRESS" &&
                                                <Tag type="cyan" title="In progress">In progress</Tag>}
                                        </td>
                                        <td>
                                            {connectorDefinition.explorationStatus !== "FAILED" &&
                                                (connectorDefinition.status === "NOT-INSTALLED" || connectorDefinition.status === "OLD" || connectorDefinition.status === "NO-RELEASE") &&
                                                <Button className="btn btn-primary btn-sm"
                                                        onClick={() => this.installConnector(connectorDefinition)}>
                                                    <CloudDownloadFill/> Install
                                                </Button>
                                            }
                                            {connectorDefinition.download &&
                                                <div style={{marginTop: "4px", fontSize: "10px"}}>
                                                    <Tag
                                                        type={connectorDefinition.download.status === "OK" ? "green" : "red"}>
                                                        {connectorDefinition.download.status}
                                                    </Tag>
                                                    <div>{connectorDefinition.download.explanation}</div>
                                                </div>
                                            }
                                        </td>
                                    </tr>
                                    {this.state.expandedRows[index] && (
                                        <tr style={this.getStyleRow(connectorDefinition)}>
                                            <td colSpan="8">
                                                <div className="card" style={{margin: "8px 0 8px 60px"}}>
                                                    <div className="card-header"><strong>Information</strong></div>
                                                    <div className="card-body" style={{fontSize: "12px"}}>
                                                        {connectorDefinition.icon && (
                                                            <div style={{
                                                                display: "flex",
                                                                alignItems: "center",
                                                                marginBottom: "6px"
                                                            }}>
                                                                <img src={connectorDefinition.icon} alt=""
                                                                     style={{
                                                                         width: "48px",
                                                                         height: "48px",
                                                                         marginRight: "12px"
                                                                     }}/>
                                                                <strong
                                                                    style={{fontSize: "14px"}}>{connectorDefinition.name}</strong>
                                                            </div>
                                                        )}
                                                        {connectorDefinition.status === "IN-PROGRESS" &&
                                                            <div><Tag type="cyan" title="In progress">In progress</Tag>
                                                            </div>}
                                                        <div><strong>Main Connector
                                                            Type:</strong> {connectorDefinition.connectorType}
                                                        </div>
                                                        <div><strong>Annotations:</strong>&nbsp;
                                                            {!connectorDefinition.listAnnotations ||
                                                            connectorDefinition.listAnnotations.length === 0 ? (
                                                                "-"
                                                            ) : (
                                                                <table style={{
                                                                    marginTop: "4px",
                                                                    marginLeft: "20px",
                                                                    borderCollapse: "collapse",
                                                                    width: "100%",
                                                                    border: "1px solid #000"
                                                                }}>
                                                                    <thead>
                                                                    <tr>
                                                                        <th style={{
                                                                            textAlign: "center",
                                                                            borderBottom: "1px solid #ccc",
                                                                            padding: "4px"
                                                                        }}>Name
                                                                        </th>
                                                                        <th style={{
                                                                            textAlign: "center",
                                                                            borderBottom: "1px solid #ccc",
                                                                            padding: "4px"
                                                                        }}>Type
                                                                        </th>
                                                                    </tr>
                                                                    </thead>
                                                                    <tbody>
                                                                    {connectorDefinition.listAnnotations.map((item, i) => (
                                                                        <tr key={i}>
                                                                            <td style={{padding: "4px"}}>{item.name}</td>
                                                                            <td style={{padding: "4px"}}>{item.type}</td>
                                                                        </tr>
                                                                    ))}
                                                                    </tbody>
                                                                </table>
                                                            )}
                                                        </div>


                                                        <div>
                                                            <strong>Description:</strong> {connectorDefinition.description}
                                                        </div>
                                                        {connectorDefinition.creator &&
                                                            <div><strong>Creator:</strong> {connectorDefinition.creator}
                                                            </div>}
                                                        <div><strong>Store:</strong> {connectorDefinition.store}</div>
                                                        <div><strong>Store
                                                            Release:</strong> {connectorDefinition.storerelease}</div>
                                                        <div><strong>Documentation:</strong>&nbsp;
                                                            {connectorDefinition.documentationRef
                                                                ? <a href={connectorDefinition.documentationRef}
                                                                     target="_blank"
                                                                     rel="noreferrer">{connectorDefinition.documentationRef}</a>
                                                                : "-"}
                                                        </div>
                                                        <div><strong>GitHub Repo:</strong>&nbsp;
                                                            {connectorDefinition.githubRepoName
                                                                ?
                                                                <a href={"https://github.com/" + connectorDefinition.githubRepoName}
                                                                   target="_blank"
                                                                   rel="noreferrer">{connectorDefinition.githubRepoName}</a>
                                                                : "-"}
                                                        </div>
                                                        <div><strong>GitHub
                                                            Path:</strong> {connectorDefinition.githubRepoPath}</div>
                                                        <div><strong>Exploration
                                                            Status:</strong> {connectorDefinition.explorationStatus}
                                                        </div>
                                                        <div><strong>Element Template URL:</strong>&nbsp;
                                                            {!connectorDefinition.elementTemplates ||
                                                            connectorDefinition.elementTemplates.length === 0 ? (
                                                                "-"
                                                            ) : (
                                                                <table style={{
                                                                    marginTop: "4px",
                                                                    marginLeft: "20px",
                                                                    borderCollapse: "collapse",
                                                                    width: "100%",
                                                                    border: "1px solid #000"
                                                                }}>
                                                                    <thead>
                                                                    <tr>
                                                                        <th style={{
                                                                            textAlign: "center",
                                                                            borderBottom: "1px solid #ccc",
                                                                            padding: "4px"
                                                                        }}>Name
                                                                        </th>
                                                                        <th style={{
                                                                            textAlign: "center",
                                                                            borderBottom: "1px solid #ccc",
                                                                            padding: "4px"
                                                                        }}>Description
                                                                        </th>
                                                                        <th style={{
                                                                            textAlign: "center",
                                                                            borderBottom: "1px solid #ccc",
                                                                            padding: "4px"
                                                                        }}>URL
                                                                        </th>
                                                                    </tr>
                                                                    </thead>
                                                                    <tbody>
                                                                    {connectorDefinition.elementTemplates.map((item, i) => (
                                                                        <tr key={i}>
                                                                            <td style={{padding: "4px"}}>{item.name}</td>
                                                                            <td style={{padding: "4px"}}>{item.description}</td>
                                                                            <td style={{padding: "4px"}}>
                                                                                <a href={item.url} target="_blank"
                                                                                   rel="noreferrer">
                                                                                    {item.url}
                                                                                </a>
                                                                            </td>
                                                                        </tr>
                                                                    ))}
                                                                    </tbody>
                                                                </table>
                                                            )}
                                                        </div>
                                                        <div><strong>Jar File URL:</strong>&nbsp;
                                                            {connectorDefinition.urlJarFile
                                                                ?
                                                                <a href={connectorDefinition.urlJarFile} target="_blank"
                                                                   rel="noreferrer">{connectorDefinition.urlJarFile}</a>
                                                                : "-"}
                                                        </div>
                                                        <div><strong>Maven URL:</strong>&nbsp;
                                                            {connectorDefinition.urlMaven
                                                                ? <a href={connectorDefinition.urlMaven} target="_blank"
                                                                     rel="noreferrer">{connectorDefinition.urlMaven}</a>
                                                                : "-"}
                                                        </div>
                                                        <div><strong>Has
                                                            Implementation:</strong> {String(connectorDefinition.hasImplementation)}
                                                        </div>
                                                    </div>
                                                </div>
                                            </td>
                                        </tr>
                                    )}
                                </React.Fragment>
                            )) : <tr>
                                <td colSpan="9"/>
                            </tr>}
                            </tbody>
                        </table>
                    </div>
                </div>
                <DownloadMessage/>

            </div>
        );
    }

    getStyleRow(connector) {
        const {statusFilter, filterSearch} = this.state.display;
        const selectedStoreNames = this.state.stores.filter(r => r.selected).map(r => r.name);
        if (!selectedStoreNames.includes(connector.store))
            return {display: "none"};
        if (statusFilter === "INSTALLABLE" && !connector.isInstallable)
            return {display: "none"};
        if (statusFilter !== "ALL" && statusFilter !== "INSTALLABLE" && connector.status !== statusFilter)
            return {display: "none"};
        if (filterSearch && !connector.name.toLowerCase().includes(filterSearch.toLowerCase()))
            return {display: "none"};
        return {};
    }

    exploreAgain() {
        this.setDisplayProperty("loading", true);
        this.setState({status: ""});
        let restCallService = RestCallService.getInstance();
        restCallService.getJson('cherry/api/store/connectors/explore?', this, this.refreshListConnectorsCallback);
    }

    refreshListConnectors() {

        const selectedRepos = this.state.stores
            .filter(r => r.selected)
            .map(r => r.name);
        const repoParams = selectedRepos.map(name => `stores=${encodeURIComponent(name)}`).join('&');
        const orderBy = this.state.display.orderBy;
        let uri = 'cherry/api/store/connectors/list?' + repoParams + '&orderBy=' + orderBy;
        console.log("Store.refreshListConnectors http[" + uri + "]");

        this.setDisplayProperty("loading", true);
        this.setState({status: ""});
        let restCallService = RestCallService.getInstance();
        restCallService.getJson(uri, this, this.refreshListConnectorsCallback);
    }

    refreshListConnectorsCallback(httpPayload) {
        console.log("refreshListConnectorsCallback");
        this.setDisplayProperty("loading", false);
        if (httpPayload.isError()) {
            console.log("Store.refreshListConnectorsCallback: error " + httpPayload.getError());
            this.setState({status: httpPayload.getError()});
        } else {
            const data = httpPayload.getData();
            const incomingStores = data.stores || [];
            const existingStores = this.state.stores;
            const existingByName = Object.fromEntries(existingStores.map(s => [s.name, s]));
            const incomingNames = new Set(incomingStores.map(s => s.name));
            const mergedStores = incomingStores.map(s =>
                existingByName[s.name] ? existingByName[s.name] : {...s, selected: true}
            ).filter(s => incomingNames.has(s.name));

            this.setDisplayProperty("loading", false);

            this.setState({
                connectors: data.connectors || [],
                stores: mergedStores
            });
        }
    }

    downloadConnector(connector) {
        let uri = 'cherry/api/store/download?name=' + connector.name;

        this.setDisplayProperty("loading", true);
        this.setState({status: ""});
        let restCallService = RestCallService.getInstance();
        restCallService.getJson(uri, this, (httpPayload) => this.downloadConnectorsCallback(httpPayload, connector));
    }

    downloadConnectorsCallback(httpPayload, connector) {
        this.setDisplayProperty("loading", false);
        if (httpPayload.isError()) {
            console.log("Store.downloadConnectorsCallback: error " + httpPayload.getError());
            this.setState({status: httpPayload.getError()});
        } else {
            const data = httpPayload.getData();
            connector.status = "INSTALLED";
            connector.release = data.release;
            this.setState({connectors: [...this.state.connectors]});
        }
    }

    installConnector(connector) {
        connector.download = null;
        this.setState({connectors: [...this.state.connectors]});
        let uri = 'cherry/api/store/connectors/install?store=' + connector.store + '&connectorname=' + connector.name + '&release=' + connector.storerelease;

        this.setDisplayProperty("loading", true);
        this.setState({status: ""});
        let restCallService = RestCallService.getInstance();
        restCallService.getJson(uri, this, (httpPayload) => this.installConnectorsCallback(httpPayload, connector));
    }

    installConnectorsCallback(httpPayload, connector) {
        this.setDisplayProperty("loading", false);
        if (httpPayload.isError()) {
            console.log("Store.downloadConnectorsCallback: error " + httpPayload.getError());
            connector.download = {status: "ERROR", explanation: httpPayload.getError()};
        } else {
            connector.download = httpPayload.getData();
        }
        this.setState({connectors: [...this.state.connectors]});
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

    setStatusFilter(status) {
        this.setDisplayProperty("statusFilter", status);
    }

    setSearchFilter(value) {
        this.setDisplayProperty("filterSearch", value);
    }

    setOrderBy(value) {
        this.setDisplayProperty("orderBy", value);
        this.setState(
            prev => ({display: {...prev.display, orderBy: value}}),
            () => this.refreshListConnectors()
        );
    }

    setDisplayProperty(propertyName, propertyValue) {
        let displayObject = this.state.display;
        displayObject[propertyName] = propertyValue;
        try {
            localStorage.setItem("store_display", JSON.stringify(displayObject));
        } catch {
        }
        this.setState({display: displayObject});
    }
}

export default Store;
