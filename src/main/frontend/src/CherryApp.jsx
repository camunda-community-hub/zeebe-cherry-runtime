// -----------------------------------------------------------
//
// CherryApps
//
// Manage the main application
//
// -----------------------------------------------------------

import React from 'react';
import './index.scss';

import 'bootstrap/dist/css/bootstrap.min.css';

import { Container, Nav, Navbar} from 'react-bootstrap';
import Dashboard from "./dashboard/Dashboard";
import Definition from "./definition/Definition";
import Parameters from "./parameter/Parameters"
import MarketPlace from "./marketplace/MarketPlace";


const FRAME_NAME = {
    DASHBOARD: "Dashboard",
    DEFINITION: "Definition",
    PARAMETERS: "Parameters",
  MARKETPLACE: "MarketPlace"
}

class CherryApp extends React.Component {


    constructor(_props) {
        super();
        this.state = {frameContent: FRAME_NAME.DASHBOARD};
        this.clickMenu = this.clickMenu.bind(this);
    }


    render() {
      return(
        <div>

          <Navbar bg="light" variant="light">
            <Container>
              <Nav className="mr-auto">
                  <Navbar.Brand href="#home">
                    <img src="/img/cherries.png" width="28" height="28" alt="cherry"/>
                    Cherry Runtime
                  </Navbar.Brand>

                <Nav.Link onClick={()=>{this.clickMenu(FRAME_NAME.DASHBOARD)}}>Dashboard</Nav.Link>
                  <Nav.Link onClick={()=>{this.clickMenu(FRAME_NAME.DEFINITION)}}>Definition</Nav.Link>
                  <Nav.Link onClick={()=>{this.clickMenu(FRAME_NAME.PARAMETERS)}}>Parameters</Nav.Link>
                <Nav.Link onClick={()=>{this.clickMenu(FRAME_NAME.MARKETPLACE)}}>Market place</Nav.Link>
              </Nav>
            </Container>
          </Navbar>
          {this.state.frameContent === FRAME_NAME.DASHBOARD && <Dashboard/>}
          {this.state.frameContent === FRAME_NAME.DEFINITION && <Definition/>}
          {this.state.frameContent === FRAME_NAME.PARAMETERS && <Parameters/>}
          {this.state.frameContent === FRAME_NAME.MARKETPLACE && <MarketPlace/>}


        </div>);
    }


    clickMenu(menu) {
        console.log("ClickMenu "+menu);
        this.setState({frameContent: menu});

    }

}

export default CherryApp;


