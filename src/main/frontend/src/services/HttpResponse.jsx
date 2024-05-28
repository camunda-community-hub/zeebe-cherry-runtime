// -----------------------------------------------------------
//
// HttpResponse
//
// Response of a Rest call
//
// -----------------------------------------------------------
class HttpResponse {

  constructor(axiosHttpPayload, err) {
    this.axiosHttpPayload = axiosHttpPayload;
    this.err = err;

  }

  isError() {
    // console.log("HttpResponse.isError: "+this.err);
    if (this.err)
      return true;
    // console.log("HttpResponse.isError httpStatus: "+this.axiosHttpPayload.status);
    if (this.axiosHttpPayload.status < 200 || this.axiosHttpPayload.status > 204)
      return true;
    return false;
  }

  getError() {
    if (this.err.response && this.err.response.data && this.err.response.data.message)
      return this.err.response.data.message;
    if (this.err.message)
      return this.err.message;
    return this.err.code;
  }

  getData() {
    if (this.axiosHttpPayload)
      return this.axiosHttpPayload.data;
    return {}
  }

  getStatus() {
    if (this.axiosHttpPayload)
      return this.axiosHttpPayload.status;
    return 0;
  }

  trace(label) {

    if (this.isError()) {
      // console.log("HttpResponse: ERROR "+JSON.stringify(this.err));
      if (this.err.response) {
        // The request was made and the server responded with a status code
        // that falls out of the range of 2xx
        console.log("HttpResponse.trace: ERROR RESP" + label + " ERROR " + this.err.response.status + " data: " + this.err.response.data);
        // console.log("HttpResponse: ERRORHEADER="+this.err.response.headers);
      } else if (this.err.request) {
        // The request was made but no response was received
        // `error.request` is an instance of XMLHttpRequest in the browser and an instance of
        // http.ClientRequest in node.js
        console.log("HttpResponse.trace: ERROR REQ" + label + " ERRORREQUEST=" + this.err.request);
      } else {
        // Something happened in setting up the request that triggered an ControllerPage
        console.log("HttpResponse.trace: ERROR ELSE " + label + " ERRORMESSAGE=", this.err.message);
      }
      // console.log(this.err.config);
    } else {
      // console.log("HttpResponse.trace: OK "+label+" "+ this.getStatus() +" "+JSON.stringify( this.getData()));
    }
  }
}

export default HttpResponse;