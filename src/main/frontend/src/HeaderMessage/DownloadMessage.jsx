// -----------------------------------------------------------
//
// DownloadMessage
//
// "Operation in progress" box: polls cherry/api/store/startup and
// displays exploration / download progress while one is running.
// Renders nothing when no operation is in progress.
//
// Used by Dashboard, Definition, Content and Store.
//
// -----------------------------------------------------------

import React from 'react';
import RestCallService from "../services/RestCallService";

class DownloadMessage extends React.Component {

    constructor(_props) {
        super();
        this.state = {
            explorationStatus: "NONE",
            percentExploration: 0,
            downloadStartup: [],
            refreshInterval: null
        };
    }

    componentDidMount() {
        this.refreshStartupStatus();
    }

    componentDidUpdate(prevProps) {
        if (prevProps.timestamp !== this.props.timestamp) {
            this.refreshStartupStatus();
        }
    }

    componentWillUnmount() {
        this.clearAutoRefresh();
    }

    isInProgress() {
        return this.state.explorationStatus === "INPROGRESS" ||
            (this.state.downloadStartup && this.state.downloadStartup.some(d => d.count < d.total));
    }

    setupAutoRefresh() {
        if (this.state.refreshInterval) {
            return;
        }
        this.scheduleNextRefresh();
    }

    scheduleNextRefresh() {
        const timeout = setTimeout(() => {
            this.refreshStartupStatus();
        }, 30000);
        this.setState({refreshInterval: timeout});
    }

    clearAutoRefresh() {
        if (this.state.refreshInterval) {
            clearTimeout(this.state.refreshInterval);
            this.setState({refreshInterval: null});
        }
    }

    refreshStartupStatus() {
        let uri = 'cherry/api/store/startup?';
        console.log("DownloadMessage.refreshStartupStatus http[" + uri + "]");
        let restCallService = RestCallService.getInstance();
        restCallService.getJson(uri, this, this.refreshStartupStatusCallback);
    }

    refreshStartupStatusCallback(httpPayload) {
        if (httpPayload.isError()) {
            console.log("DownloadMessage.refreshStartupStatusCallback: error " + httpPayload.getError());
            return;
        }
        const data = httpPayload.getData();
        const explorationStatus = data.exploration ? data.exploration.status : "NONE";
        const percentExploration = data.exploration ? data.exploration.percent : 0;
        this.setState({
            explorationStatus: explorationStatus,
            downloadStartup: data.downloadStartup || [],
            percentExploration: percentExploration
        }, () => {
            if (this.isInProgress()) {
                this.setupAutoRefresh();
            } else {
                this.clearAutoRefresh();
            }
        });
    }

    render() {
        const {explorationStatus, percentExploration, downloadStartup} = this.state;
        const hasDownloads = downloadStartup && downloadStartup.length > 0;

        if (explorationStatus !== "INPROGRESS" && !hasDownloads) {
            return null;
        }

        return (
            <div className="row" style={{width: "100%", marginTop: "16px"}}>
                <div className="col-md-12">
                    <div className="card" style={{borderLeft: "4px solid #0043CE"}}>
                        <div className="card-header cherry-header">Operation in progress</div>
                        <div className="card-body" style={{fontSize: "12px"}}>
                            {explorationStatus === "INPROGRESS" && (
                                <div style={{
                                    marginBottom: "12px",
                                    fontStyle: "italic",
                                    color: "#0043CE",
                                    fontWeight: "bold"
                                }}>
                                    Exploration in progress... ({percentExploration || 0}%)
                                </div>
                            )}
                            {hasDownloads && (
                                <div>
                                    <div style={{marginBottom: "12px", color: "#0043CE", fontWeight: "bold"}}>
                                        Download in progress
                                    </div>
                                    {downloadStartup.map((download, idx) => (
                                        <div key={idx} style={{marginBottom: "4px"}}>
                                            {download.name}: {download.count} / {download.total} ({download.percentage}%)
                                            {download.currentDownloadName && ` : ${download.currentDownloadName}`}
                                            {download.count >= 1 && download.count < download.total && " In progress"}
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        );
    }
}

export default DownloadMessage;
