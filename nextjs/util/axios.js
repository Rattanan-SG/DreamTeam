import axios from 'axios'

const instance = axios.create({
  baseURL: 'https://ngelearning.sit.kmutt.ac.th/api/v0',
  headers: {
  }
});

export default instance