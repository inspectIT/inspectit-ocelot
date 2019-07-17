import axios from "axios";

const axiosInstance = axios.create();

axiosInstance.defaults.timeout = 5000;

export default axiosInstance;