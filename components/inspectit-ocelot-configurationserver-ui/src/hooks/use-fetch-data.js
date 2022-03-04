import { useState, useEffect } from 'react';
import axios from '../lib/axios-api';

/**
 * Fetches data from the specified endpoint. The URL is relative to the API's base url.
 * See 'lib/axios-api' for more information.
 */
export default (url, params, initialData = null) => {
  const [lastUpdate, setLastUpdate] = useState(null);
  const [counter, setCounter] = useState(0);
  const [data, setData] = useState(initialData);
  const [isLoading, setIsLoading] = useState(false);
  const [isError, setIsError] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      if (counter === 0) {
        return;
      }
      setIsError(false);
      setIsLoading(true);

      try {
        const result = await axios.get(url, { params });

        setData(result.data);
        setError(null);
      } catch (error) {
        setIsError(true);
        setError(error);
      }

      setLastUpdate(Date.now());
      setIsLoading(false);
    };

    fetchData();
  }, [counter]);

  const refresh = () => setCounter(counter + 1);

  return [{ data, isLoading, isError, lastUpdate, error }, refresh];
};
