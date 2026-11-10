// -----------------------------------------------------------
//
// JobCount
//
// Jobs page: same look and feel as the Dashboard, focused on the
// number of pending jobs per runner.
//
// -----------------------------------------------------------

import React from 'react';
import {Button, Select, TextInput} from "carbon-components-react";
import {ArrowRepeat} from 'react-bootstrap-icons';

import RunnerCartouche from "../component/RunnerCartouche";
import RunnerAnalysisTags from "../dashboard/RunnerAnalysisTags";
import RunnerChart from "../dashboard/RunnerChart";
import ControllerPage from "../component/ControllerPage";
import DownloadMessage from "../HeaderMessage/DownloadMessage";
import RestCallService from "../services/RestCallService";

class JobCount extends React.Component {

    constructor(_props) {
        super();
        this.downloadMessageRef = React.createRef();
        this.state = {
            dashboard: {
                details: [],
            },
            display: {
                loading: true,
                filterValue: "all",
                orderBy: "nameAsc",
                period: "ONEDAY",
                filterSearch: "",
                showWorker: true,
                showConnector: true,
                showUndefined: true
            },
            status: ""
        };
    }

    componentDidMount() {
        this.refreshJobCount();
    }

    render() {
        return (<div className={"container"}>
            <DownloadMessage ref={this.downloadMessageRef} timestamp={this.state.dashboard.timestamp}
                             onError={(err) => this.setState({status: err})}/>
            <div className="row">
                <div className="col-md-1">
                    <h1 className="title">Jobs</h1>
                </div>
                <div className="row" style={{width: "100%"}}>
                    <div className="col-md-12">
                        <ControllerPage errorMessage={this.state.status} loading={this.state.display.loading}/>
                    </div>
                </div>

                <div className="col-md-6">
                    <div className="btn-group" role="group" style={{padding: "10px 10px 10px 10px"}}>
                        <button className={this.getButtonClass(this.state.display.filterValue === "all")}
                                style={{marginLeft: "10px", fontSize: "10px", height: "40px"}}
                                disabled={this.state.display.loading}
                                onClick={() => this.setFilterValue("all")}>
                            All
                        </button>
                        <button className={this.getButtonClass(this.state.display.filterValue === "onboard")}
                                style={{fontSize: "10px", height: "40px"}}
                                disabled={this.state.display.loading}
                                onClick={() => this.setFilterValue("onboard")}>
                            Onboard
                        </button>

                        <button className={this.getButtonClass(this.state.display.showWorker)}
                                style={{marginLeft: "10px", fontSize: "10px", height: "40px"}}
                                disabled={this.state.display.loading}
                                onClick={() => this.setToggleFilter("showWorker")}>
                            Worker
                        </button>
                        <button className={this.getButtonClass(this.state.display.showConnector)}
                                style={{fontSize: "10px", height: "40px"}}
                                disabled={this.state.display.loading}
                                onClick={() => this.setToggleFilter("showConnector")}>
                            Connector
                        </button>
                        <button className={this.getButtonClass(this.state.display.showUndefined)}
                                style={{fontSize: "10px", height: "40px"}}
                                disabled={this.state.display.loading}
                                onClick={() => this.setToggleFilter("showUndefined")}>
                            Undefined
                        </button>
                    </div>
                </div>
                <div className="col-md-1">
                    <TextInput labelText="Filter"
                               disabled={this.state.display.loading}
                               value={this.state.display.filterSearch}
                               style={{width: "80px"}}
                               onChange={(event) => this.setSearchFilter(event.target.value)}/>
                </div>
                <div className="col-md-2">
                    <Select
                        value={this.state.display.orderBy}
                        labelText="order by"
                        disabled={this.state.display.loading}
                        onChange={(event) => this.setOrderBy(event.target.value)}>
                        <option value="nameAsc">Name (asc)</option>
                        <option value="nameDes">Name (desc)</option>
                        <option value="jobsAsc">Jobs (asc)</option>
                        <option value="jobsDes">Jobs (desc)</option>
                    </Select>
                </div>
                <div className="col-md-auto">
                    <Select
                        value={this.state.display.period}
                        labelText="Period"
                        disabled={this.state.display.loading}
                        style={{width: "150px"}}
                        onChange={(event) => this.setPeriod(event.target.value)}>
                        <option value="FOURHOUR">Last 4 Hours</option>
                        <option value="ONEDAY">last 24 Hours</option>
                        <option value="ONEWEEK">Last week</option>
                        <option value="ONEMONTH">Last Month</option>
                    </Select>
                </div>
                <div className="col-md-1">
                    <Button className="btn btn-success btn-sm"
                            onClick={() => {
                                this.refreshJobCount();
                                this.downloadMessageRef.current?.refreshStartupStatus();
                            }}
                            disabled={this.state.display.loading}>
                        <ArrowRepeat/> Refresh
                    </Button>
                </div>
            </div>

            <div className="row" style={{width: "100%"}}>
                <div className="col-md-12">
                    <table width="100%" style={{tableLayout: "fixed", borderCollapse: "collapse"}}>
                        <tbody>
                        {this.getSortedFilteredDetails().map((runner, _index) =>
                            <tr key={runner.type} style={{borderBottom: "1px solid #dee2e6"}}>
                                <td style={{verticalAlign: "top", width: "120px", paddingTop: "10px", paddingBottom: "10px"}}>
                                    <RunnerCartouche runner={runner}/>
                                </td>
                                <td style={{verticalAlign: "top", width: "20%", paddingTop: "10px", paddingBottom: "10px"}}>
                                    <h1 className="title is-5">{runner.name}</h1>
                                    <div style={{fontStyle: "italic", fontSize: "small"}}>
                                        connector type: {runner.connectorType}
                                    </div>
                                </td>
                                <td style={{verticalAlign: "top", width: "190px", paddingTop: "10px", paddingBottom: "10px"}}>
                                    <RunnerAnalysisTags runnerDisplay={runner} timestamp={this.state.dashboard.timestamp}/>
                                </td>
                                <td style={{verticalAlign: "top", paddingTop: "10px", paddingBottom: "10px", paddingLeft: "10px"}}>
                                    <RunnerChart type="ExecutionsTopic" runnerDisplay={runner} title={false}
                                                 timestamp={this.state.dashboard.timestamp}/>
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>)
    }

    getButtonClass(active) {
        if (active)
            return "btn btn-primary btn-sm";
        return "btn btn-outline-primary btn-sm"
    }

    setFilterValue(value) {
        let displayObject = this.state.display;
        displayObject.filterValue = value;
        this.setState({display: displayObject});
        this.refreshJobCount();
    }

    setToggleFilter(propertyName) {
        let value = this.state.display[propertyName];
        this.setDisplayProperty(propertyName, !value);
    }

    setSearchFilter(value) {
        this.setDisplayProperty("filterSearch", value);
    }

    setOrderBy(value) {
        this.setDisplayProperty("orderBy", value);
    }

    setPeriod(value) {
        this.setDisplayProperty("period", value);
        this.refreshJobCount();
    }

    /**
     * Sorting/searching/class filtering is done client side: the backend returns the full "details"
     * list for the selected onboard/all filter, already enriched with everything needed to display,
     * filter and sort it.
     */
    getSortedFilteredDetails() {
        let listDetails = this.state.dashboard.details || [];
        listDetails = listDetails.filter(runner => {
            if (runner.classrunner === "worker") return this.state.display.showWorker;
            if (runner.classrunner === "connector") return this.state.display.showConnector;
            return this.state.display.showUndefined;
        });
        if (this.state.display.filterSearch) {
            const search = this.state.display.filterSearch.toLowerCase();
            listDetails = listDetails.filter(runner =>
                (runner.name && runner.name.toLowerCase().includes(search))
                || (runner.type && runner.type.toLowerCase().includes(search)));
        }
        const comparators = {
            nameAsc: (a, b) => (a.name || "").localeCompare(b.name || ""),
            nameDes: (a, b) => (b.name || "").localeCompare(a.name || ""),
            jobsAsc: (a, b) => (a.topicCount || 0) - (b.topicCount || 0),
            jobsDes: (a, b) => (b.topicCount || 0) - (a.topicCount || 0)
        };
        return [...listDetails].sort(comparators[this.state.display.orderBy]);
    }

    refreshJobCount() {
        let uri = 'cherry/api/jobcount/dashboard?filter=' + this.state.display.filterValue
            + '&period=' + this.state.display.period;
        console.log("JobCount.refreshJobCount http[" + uri + "]");

        this.setDisplayProperty("loading", true);
        this.setState({status: ""});
        var restCallService = RestCallService.getInstance();
        restCallService.getJson(uri, this, this.refreshJobCountCallback);
    }

    refreshJobCountCallback(httpPayload) {
        this.setDisplayProperty("loading", false);
        if (httpPayload.isError()) {
            console.log("JobCount.refreshJobCountCallback: error " + httpPayload.getError());
            this.setState({status: httpPayload.getError()});
        } else {
            this.setState({dashboard: httpPayload.getData()});
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

export default JobCount;
