// -----------------------------------------------------------
//
// Parameters
//
// List of all runners available
//
// -----------------------------------------------------------

import React from 'react';
import {Button} from "carbon-components-react";
import {ArrowRepeat} from "react-bootstrap-icons";

import RestCallService from "../services/RestCallService";
import ControllerPage from "../component/ControllerPage";

class Tenants extends React.Component {


    constructor(_props) {
        super();
        this.state = {
            connectors: [],
            display: {loading: false}
        };
    }

    componentDidMount(prevProps) {
        this.refreshListTenants();

    }

    render() {
        return (
            <div class={"container"}>
                <div className="row" style={{width: "100%"}}>
                    <div className="col-md-10">
                        <h1 className="title">Tenants</h1>
                    </div>
                    <div className="col-md-2">
                        <Button className="btn btn-success btn-sm"
                                onClick={() => this.refreshListTenants()}
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
                            Tenants
                        </div>
                    </div>
                </div>
            </div>
        )
    }

    getStyleRow(connectorMarket) {
        return {};
    }

    refreshListTenants(period, orderBy) {
        let uri = 'cherry/api/tenants/list?';
        console.log("Store.refreshListTenants http[" + uri + "]");

        this.setDisplayProperty("loading", true);
        this.setState({status: ""});
        let restCallService = RestCallService.getInstance();
        restCallService.getJson(uri, this, this.refreshListTenantsCallback);
    }

    refreshListTenantsCallback(httpPayload) {
        this.setDisplayProperty("loading", false);
        if (httpPayload.isError()) {
            console.log("Store.refreshListTenantsCallback: error " + httpPayload.getError());
            this.setState({status: "ControllerPage " + httpPayload.getError()});
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

export default Tenants;