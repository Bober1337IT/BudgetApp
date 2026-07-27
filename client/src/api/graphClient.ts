import axios from "axios";
import { GRAPHQL_URL } from "../config/apiConfig";

interface GraphQLError {
  message: string;
}

interface GraphQLResponse<T = unknown> {
  data: T;
  errors?: GraphQLError[];
}

const graphqlClient = async <T = unknown>(
  query: string,
  variables?: Record<string, unknown>
): Promise<GraphQLResponse<T>> => {
  const token = localStorage.getItem("accessToken");

  const response = await axios.post<GraphQLResponse<T>>(
    GRAPHQL_URL,
    { query, variables },
    {
      headers: {
        "Content-Type": "application/json",
        Authorization: token ? `Bearer ${token}` : "",
      },
    }
  );

  if (response.data.errors && response.data.errors.length > 0) {
    throw new Error(response.data.errors.map((e) => e.message).join("\n"));
  }

  return response.data;
};

export default graphqlClient;
