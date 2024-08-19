// -----------------------------------------------------------
//
// HeaderMessage
//
// Display message (alert message) in the header
//
// -----------------------------------------------------------

import React from 'react';



import RestCallService from "../services/RestCallService";



class HeaderMessage extends React.Component {


  constructor(_props) {
    super();
    this.state = {
      zeebeConnection: true,
      counter: 0,
      display: {
        loading: true,

      },


    };
  }

  componentDidMount() {
    const intervalId = setInterval( this.performAction, 60000);

  }

  performAction = () => {
    console.log("Action executed");
    this.setState({counter: this.state.counter+1})
  };

  render() {
    // console.log("dashboard.render display="+JSON.stringify(this.state.display));
    return (<div className={"container"}>
      </div>
    );
  }
}

export default HeaderMessage;
