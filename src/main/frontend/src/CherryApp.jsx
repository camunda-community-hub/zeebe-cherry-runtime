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

import {Container, Nav, Navbar} from 'react-bootstrap';
import Dashboard from "./dashboard/Dashboard";
import Definition from "./definition/Definition";
import Parameters from "./parameter/Parameters"
import Secrets from "./secrets/Secrets"
import Content from "./content/Content"
import Store from "./store/Store"
import OperationLog from "./operationlog/OperationLog"

const FRAME_NAME = {
  DASHBOARD: "Dashboard",
  DEFINITION: "Definition",
  SECRET: "Secret",
  ENVIRONMENT: "Environment",
  CONTENT: "Content",
  PARAMETERS: "Parameters",
  STORE: "Store",
  OPERATIONLOG : "OperationLog"

}

class CherryApp extends React.Component {


  constructor(_props) {
    super();
    this.state = {frameContent: FRAME_NAME.DASHBOARD};
    this.clickMenu = this.clickMenu.bind(this);
  }


  render() {
    return (
      <div>

        <Navbar bg="light" variant="light">
          <Container>
            <Nav className="mr-auto">
              <Navbar.Brand href="#home">
                <img src="/img/cherries.png" width="28" height="28" alt="cherry"/>
                Cherry Runtime
              </Navbar.Brand>

              <Nav.Link onClick={() => {
                this.clickMenu(FRAME_NAME.DASHBOARD)
              }}>Dashboard</Nav.Link>

              <Nav.Link onClick={() => {
                this.clickMenu(FRAME_NAME.DEFINITION)
              }}>Definition</Nav.Link>

              <Nav.Link onClick={() => {
                this.clickMenu(FRAME_NAME.SECRET)
              }}>Secrets</Nav.Link>

              <Nav.Link onClick={() => {
                this.clickMenu(FRAME_NAME.CONTENT)
              }}>Content</Nav.Link>

              <Nav.Link onClick={() => {
                this.clickMenu(FRAME_NAME.STORE)
              }}>Store</Nav.Link>

              <Nav.Link onClick={() => {
                this.clickMenu(FRAME_NAME.OPERATIONLOG)
              }}>Log</Nav.Link>

              <Nav.Link onClick={() => {
                this.clickMenu(FRAME_NAME.PARAMETERS)
              }}>Parameters</Nav.Link>
            </Nav>
          </Container>
        </Navbar>
        {this.state.frameContent === FRAME_NAME.DASHBOARD && <Dashboard/>}
        {this.state.frameContent === FRAME_NAME.DEFINITION && <Definition/>}
        {this.state.frameContent === FRAME_NAME.SECRET && <Secrets/>}
        {this.state.frameContent === FRAME_NAME.CONTENT && <Content/>}
        {this.state.frameContent === FRAME_NAME.STORE && <Store/>}
        {this.state.frameContent === FRAME_NAME.OPERATIONLOG && <OperationLog/>}
        {this.state.frameContent === FRAME_NAME.PARAMETERS && <Parameters/>}


      </div>);
  }


  clickMenu(menu) {
    console.log("ClickMenu " + menu);
    this.setState({frameContent: menu});

  }

}

export default CherryApp;


