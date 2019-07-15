import axios from "axios";

const axiosInstance = axios.create();

axiosInstance.defaults.timeout = 2500;

// response interceptor
axiosInstance.interceptors.response.use(function (response) {
    return response;
}, function (error) {
    return Promise.reject(error);
});

export default axiosInstance;