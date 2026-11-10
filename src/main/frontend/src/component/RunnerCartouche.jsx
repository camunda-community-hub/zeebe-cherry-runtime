// -----------------------------------------------------------
//
// RunnerCartouche
//
// Compact badge for a runner: logo + Started/Stopped button.
// Clicking it opens the runner's detail modal (header, monitoring, start/stop command).
//
// -----------------------------------------------------------

import React from 'react';
import {Button, Modal} from "react-bootstrap";
import RunnerMonitoring from "../dashboard/RunnerMonitoring";
import RunnerHeader from "../definition/RunnerHeader";
import RestCallService from "../services/RestCallService";

class RunnerCartouche extends React.Component {


    constructor(props) {
        super();

        this.state = {
            runner: props.runner,
            isOpen: false,
            runnerDetail: null
        };
        this.openModal = this.openModal.bind(this);
        this.closeModal = this.closeModal.bind(this);
    }

    componentDidUpdate(prevProps) {
        if (prevProps.runner !== this.props.runner) {
            this.setState({runner: this.props.runner});
        }
    }

    render() {
        return (
            <div>
                <table
                    style={{
                        border: "1px solid",
                        padding: "5px",
                        textAlign: "center",
                        boxShadow: "5px 5px",
                        cursor: "pointer"
                    }}
                    onClick={() => this.openModal()}>
                    <tbody>
                    <tr>
                        <td><img width="30px" src={this.state.runner.logo}
                                 alt={this.state.runner.name}/></td>
                    </tr>
                    <tr>
                        <td style={{border: "1px solid", padding: "0px"}}>
                            {this.state.runner.runnerState === "ACTIVE" &&
                                <button
                                    className="start-runner button is-selected is-primary"
                                    style={{
                                        height: "1.5em",
                                        border: "1px solid"
                                    }}>Started</button>}
                            {this.state.runner.runnerState === "UNREGISTERED" &&
                                <button
                                    className="button is-selected"
                                    style={{
                                        height: "1.5em",
                                        border: "1px solid",
                                        backgroundColor: "#999999",
                                        color: "white"
                                    }}>Unknown</button>}
                            {this.state.runner.runnerState !== "ACTIVE" && this.state.runner.runnerState !== "UNREGISTERED" &&
                                <button
                                    className="stop-runner button is-selected is-danger"
                                    style={{height: "1.5em"}}>Stopped</button>}
                        </td>
                    </tr>
                    </tbody>
                </table>

                <Modal show={this.state.isOpen} onHide={this.closeModal}
                       dialogClassName="modal-90w"
                       aria-labelledby="example-custom-modal-styling-title"
                       backdrop="static"
                >
                    <Modal.Header closeButton>
                        <Modal.Title>
                            <RunnerHeader runner={this.state.runner}/>
                        </Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        {this.state.runner.runnerState === "UNREGISTERED"
                            ? <div>No monitoring available: this job type is not registered in Cherry.</div>
                            : (this.state.runnerDetail
                                ? <RunnerMonitoring runnerDisplay={this.state.runnerDetail}/>
                                : <div>Loading...</div>)}
                    </Modal.Body>
                    <Modal.Footer>
                        <Button variant="secondary" onClick={this.closeModal}>
                            Close
                        </Button>
                    </Modal.Footer>
                </Modal>
            </div>);
    }

    openModal() {
        console.log("RunnerCartouche.openModal");
        this.setState({isOpen: true});
        if (this.state.runner.runnerState !== "UNREGISTERED") {
            this.loadRunnerDetail();
        }
    }

    closeModal() {
        console.log("RunnerCartouche.closeModal");
        this.setState({isOpen: false});
    }

    /**
     * The collapsed badge only needs {name, type, logo, runnerState} (whatever the caller already
     * has at hand). The modal's RunnerMonitoring needs the full runner record (statistic,
     * performance, threshold, ...), so it's loaded on demand when the modal actually opens.
     */
    loadRunnerDetail() {
        let uri = 'cherry/api/runner/detail?runnertype=' + this.state.runner.type + '&stats=true';
        console.log("RunnerCartouche.loadRunnerDetail http[" + uri + "]");
        var restCallService = RestCallService.getInstance();
        restCallService.getJson(uri, this, this.loadRunnerDetailCallback);
    }

    loadRunnerDetailCallback(httpPayload) {
        if (httpPayload.isError()) {
            console.log("RunnerCartouche.loadRunnerDetailCallback: error " + httpPayload.getError());
        } else {
            this.setState({runnerDetail: httpPayload.getData()});
        }
    }

}

export default RunnerCartouche;
