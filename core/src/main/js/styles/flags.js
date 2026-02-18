// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import { css } from "lit";

const flags = ["at", "be", "de", "dk", "es", "fi", "fr", "nl", "si", "us", "ca"];

export function hasFlag(country) {
  return flags.includes(country?.toLowerCase());
}

/**
 * Flag icons of all supported countries from https://github.com/twitter/twemoji.
 */
export const flagStyles = css`
  .flag {
    display: inline-block;
    width: 1em;
    height: 1em;
    background-size: contain;
    background-repeat: no-repeat;
    vertical-align: middle;
  }

  .flag-at {
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 36 36'%3E%3Cpath fill='%23EEE' d='M0 13h36v10H0z'/%3E%3Cpath fill='%23ED2939' d='M32 5H4C1.791 5 0 6.791 0 9v4h36V9c0-2.209-1.791-4-4-4zM4 31h28c2.209 0 4-1.791 4-4v-4H0v4c0 2.209 1.791 4 4 4z'/%3E%3C/svg%3E");
  }

  .flag-be {
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 36 36' xml:space='preserve'%3E%3Cpath fill='%23141414' d='M7 5a4 4 0 0 0-4 4v18a4 4 0 0 0 4 4h6V5H7z'/%3E%3Cpath fill='%23FDDA24' d='M13 5h10v26H13z'/%3E%3Cpath fill='%23EF3340' d='M29 5h-6v26h6a4 4 0 0 0 4-4V9a4 4 0 0 0-4-4z'/%3E%3C/svg%3E");
  }

  .flag-de {
    background-image: url("data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzNiIgaGVpZ2h0PSIzNiIgdmlld0JveD0iMCAwIDM2IDM2Ij48cGF0aCBmaWxsPSIjZmZjZDA1IiBkPSJNMCAyN2E0IDQgMCAwIDAgNCA0aDI4YTQgNCAwIDAgMCA0LTR2LTRIMHoiLz48cGF0aCBmaWxsPSIjZWQxZjI0IiBkPSJNMCAxNGgzNnY5SDB6Ii8+PHBhdGggZmlsbD0iIzE0MTQxNCIgZD0iTTMyIDVINGE0IDQgMCAwIDAtNCA0djVoMzZWOWE0IDQgMCAwIDAtNC00Ii8+PC9zdmc+");
  }

  .flag-dk {
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 36 36'%3E%3Cpath fill='%23C60C30' d='M32 5H15v11h21V9c0-2.209-1.791-4-4-4zM15 31h17c2.209 0 4-1.791 4-4.5V20H15v11zM0 20v6.5C0 29.209 1.791 31 4 31h7V20H0zM11 5H4C1.791 5 0 6.791 0 9v7h11V5z'/%3E%3Cpath fill='%23EEE' d='M15 5h-4v11H0v4h11v11h4V20h21v-4H15z'/%3E%3C/svg%3E");
  }

  .flag-es {
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 36 36'%3E%3Cpath fill='%23C60A1D' d='M36 27c0 2.209-1.791 4-4 4H4c-2.209 0-4-1.791-4-4V9c0-2.209 1.791-4 4-4h28c2.209 0 4 1.791 4 4v18z'/%3E%3Cpath fill='%23FFC400' d='M0 12h36v12H0z'/%3E%3Cpath fill='%23EA596E' d='M9 17v3c0 1.657 1.343 3 3 3s3-1.343 3-3v-3H9z'/%3E%3Cpath fill='%23F4A2B2' d='M12 16h3v3h-3z'/%3E%3Cpath fill='%23DD2E44' d='M9 16h3v3H9z'/%3E%3Cellipse fill='%23EA596E' cx='12' cy='14.5' rx='3' ry='1.5'/%3E%3Cellipse fill='%23FFAC33' cx='12' cy='13.75' rx='3' ry='.75'/%3E%3Cpath fill='%2399AAB5' d='M7 16h1v7H7zm9 0h1v7h-1z'/%3E%3Cpath fill='%2366757F' d='M6 22h3v1H6zm9 0h3v1h-3zm-8-7h1v1H7zm9 0h1v1h-1z'/%3E%3C/svg%3E");
  }

  .flag-fi {
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 36 36'%3E%3Cpath fill='%23EDECEC' d='M32 5H18v10h18V9c0-2.209-1.791-4-4-4z'/%3E%3Cpath fill='%23EEE' d='M11 5H4C1.791 5 0 6.791 0 9v6h11V5z'/%3E%3Cpath fill='%23EDECEC' d='M32 31H18V21h18v6c0 2.209-1.791 4-4 4zm-21 0H4c-2.209 0-4-1.791-4-4v-6h11v10z'/%3E%3Cpath fill='%23003580' d='M18 5h-7v10H0v6h11v10h7V21h18v-6H18z'/%3E%3C/svg%3E");
  }

  .flag-fr {
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 36 36'%3E%3Cpath fill='%23ED2939' d='M36 27c0 2.209-1.791 4-4 4h-8V5h8c2.209 0 4 1.791 4 4v18z'/%3E%3Cpath fill='%23002495' d='M4 5C1.791 5 0 6.791 0 9v18c0 2.209 1.791 4 4 4h8V5H4z'/%3E%3Cpath fill='%23EEE' d='M12 5h12v26H12z'/%3E%3C/svg%3E");
  }

  .flag-nl {
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 36 36'%3E%3Cpath fill='%23EEE' d='M0 14h36v8H0z'/%3E%3Cpath fill='%23AE1F28' d='M32 5H4C1.791 5 0 6.791 0 9v5h36V9c0-2.209-1.791-4-4-4z'/%3E%3Cpath fill='%2320478B' d='M4 31h28c2.209 0 4-1.791 4-4v-5H0v5c0 2.209 1.791 4 4 4z'/%3E%3C/svg%3E");
  }

  .flag-si {
    background-image: url("data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzNiIgaGVpZ2h0PSIzNiIgdmlld0JveD0iMCAwIDM2IDM2Ij48cGF0aCBmaWxsPSIjZWQxYzIzIiBkPSJNMzYgMjd2LTRIMHY0YTQgNCAwIDAgMCA0IDRoMjhhNCA0IDAgMCAwIDQtNCIvPjxwYXRoIGZpbGw9IiNlZWUiIGQ9Ik0zNiAyM0gwVjlhNCA0IDAgMCAxIDQtNGgyOGE0IDQgMCAwIDEgNCA0eiIvPjxwYXRoIGZpbGw9IiMwMDVkYTQiIGQ9Ik0wIDEzaDM2djEwSDB6Ii8+PHBhdGggZmlsbD0iI2VkMWMyMyIgZD0iTTExLjEyNSA3LjkxN2MtMi4yNSAwLTMuODMzLjgzMy0zLjgzMy44MzNzLjE0NiAyIC4zMzMgNS4wODNzMy41IDQuMTY3IDMuNSA0LjE2N3MzLjMxMi0xLjA4MyAzLjUtNC4xNjdzLjMzMy01LjA4My4zMzMtNS4wODNzLTEuNTgzLS44MzMtMy44MzMtLjgzMyIvPjxwYXRoIGZpbGw9IiMwMDRhNzciIGQ9Ik0xNC41OTIgOC41ODZjLS41ODgtLjI0Mi0xLjg0OS0uNjctMy40NjctLjY3cy0yLjg3OS40MjgtMy40NjcuNjdjLjAxMS4yMS4xMzcgMi41MDMuMjk5IDUuMTY0Yy4xNyAyLjc5MSAzLjE2NyAzLjc3MSAzLjE2NyAzLjc3MXMyLjk5OC0uOTggMy4xNjctMy43NzFjLjE2NC0yLjY2LjI5LTQuOTU0LjMwMS01LjE2NCIvPjxwYXRoIGZpbGw9IiNmZmYiIGQ9Ik0xMi4xMDQgMTUuOTJjLS4zNTQgMC0uNTIxLjIxMS0xLjA0Mi4yMTFzLS42MDQtLjIxMS0uOTU4LS4yMTFjLS4yNjggMC0uNDM0LjEyLS42MzkuMTc5Yy44MTIuNjEgMS42Ni44NiAxLjY2Ljg2Yy43MTEtLjExOCAxLjI3LS40NjYgMS42OTMtLjg1OWMtLjI2OS0uMDU5LS40NDUtLjE4LS43MTQtLjE4bS0xLjk1OC0xLjM4M2MuMzMzIDAgLjYyNS4yNi45NzkuMjZzLjYwNC0uMjYuOTc5LS4yNmMuMzIxIDAgLjc0My40MTkgMS4zNi4xNzlhNS40IDUuNCAwIDAgMCAuNDExLS44NDFsLTEuMjUtMS43OTJsLS42MjUuNzU5bC0uODc1LTEuNjc1bC0uODMzIDEuNjc1bC0uNjA0LS43OTlzLS41NDIuNjQzLTEuNDM4IDEuODUxYy4xMDcuMjg2LjI1MS41MzQuNDA3Ljc2NmMuNzA5LjM1IDEuMTg3LS4xMjMgMS40ODktLjEyM20yLjk1OC43NTVjLS40NTggMC0uNjQ2LS4yNi0xLS4yNnMtLjUyMS4yNi0xLjA0Mi4yNnMtLjYwNC0uMjYtLjk1OC0uMjZzLS41My4yNi0uODU0LjI2Yy0uMTE3IDAtLjI0OC0uMDM2LS4zNzMtLjA4NWMuMTI3LjE2OC4yNTIuMzQxLjM5LjQ4NGMuMzg2LS4wMzUuNjczLS4yNzMuODc5LS4yNzNjLjMzMyAwIC42MjUuMjc4Ljk3OS4yNzhzLjYwNC0uMjc4Ljk3OS0uMjc4Yy4yMzEgMCAuNTE2LjIzNS44ODcuMjcxcS4xNjItLjIxMy4yOTMtLjQyM2MtLjA1OC4wMDktLjExMy4wMjYtLjE4LjAyNiIvPjxwYXRoIGZpbGw9IiNmZDAiIGQ9Im0xMC4zMTggOC44MDdsLjIxNy4yMzNsLS4zMDktLjA3MmwtLjA5NC4zMDRsLS4wOTItLjMwNGwtLjMxMS4wNzJsLjIxOC0uMjMzbC0uMjE4LS4yMzNsLjMxMS4wNzJsLjA5Mi0uMzA0bC4wOTQuMzA0bC4zMDktLjA3MnptMi4wOTQgMGwuMjE3LjIzM2wtLjMxLS4wNzJsLS4wOTMuMzA0bC0uMDkzLS4zMDRsLS4zMS4wNzJsLjIxNy0uMjMzbC0uMjE3LS4yMzNsLjMxLjA3MmwuMDkzLS4zMDRsLjA5My4zMDRsLjMxLS4wNzJ6bS0xLjA4NCAxLjM5NmwuMjE2LjIzM2wtLjMwOS0uMDcybC0uMDkzLjMwM2wtLjA5My0uMzAzbC0uMzEuMDcybC4yMTctLjIzM2wtLjIxNy0uMjMzbC4zMS4wNzJsLjA5My0uMzA0bC4wOTMuMzA0bC4zMDktLjA3MnoiLz48L3N2Zz4=");
  }

  .flag-us {
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 36 36'%3E%3Cpath fill='%23B22334' d='M35.445 7C34.752 5.809 33.477 5 32 5H18v2h17.445zM0 25h36v2H0zm18-8h18v2H18zm0-4h18v2H18zM0 21h36v2H0zm4 10h28c1.477 0 2.752-.809 3.445-2H.555c.693 1.191 1.968 2 3.445 2zM18 9h18v2H18z'/%3E%3Cpath fill='%23EEE' d='M.068 27.679c.017.093.036.186.059.277.026.101.058.198.092.296.089.259.197.509.333.743L.555 29h34.89l.002-.004c.135-.233.243-.483.332-.741.034-.099.067-.198.093-.301.023-.09.042-.182.059-.275.041-.22.069-.446.069-.679H0c0 .233.028.458.068.679zM0 23h36v2H0zm0-4v2h36v-2H18zm18-4h18v2H18zm0-4h18v2H18zM0 9c0-.233.03-.457.068-.679C.028 8.542 0 8.767 0 9zm.555-2l-.003.005L.555 7zM.128 8.044c.025-.102.06-.199.092-.297-.034.098-.066.196-.092.297zM18 9h18c0-.233-.028-.459-.069-.68-.017-.092-.035-.184-.059-.274-.027-.103-.059-.203-.094-.302-.089-.258-.197-.507-.332-.74.001-.001 0-.003-.001-.004H18v2z'/%3E%3Cpath fill='%233C3B6E' d='M18 5H4C1.791 5 0 6.791 0 9v10h18V5z'/%3E%3Cpath fill='%23FFF' d='M2.001 7.726l.618.449-.236.725L3 8.452l.618.448-.236-.725L4 7.726h-.764L3 7l-.235.726zm2 2l.618.449-.236.725.617-.448.618.448-.236-.725L6 9.726h-.764L5 9l-.235.726zm4 0l.618.449-.236.725.617-.448.618.448-.236-.725.618-.449h-.764L9 9l-.235.726zm4 0l.618.449-.236.725.617-.448.618.448-.236-.725.618-.449h-.764L13 9l-.235.726zm-8 4l.618.449-.236.725.617-.448.618.448-.236-.725.618-.449h-.764L5 13l-.235.726zm4 0l.618.449-.236.725.617-.448.618.448-.236-.725.618-.449h-.764L9 13l-.235.726zm4 0l.618.449-.236.725.617-.448.618.448-.236-.725.618-.449h-.764L13 13l-.235.726zm-6-6l.618.449-.236.725L7 8.452l.618.448-.236-.725L8 7.726h-.764L7 7l-.235.726zm4 0l.618.449-.236.725.617-.448.618.448-.236-.725.618-.449h-.764L11 7l-.235.726zm4 0l.618.449-.236.725.617-.448.618.448-.236-.725.618-.449h-.764L15 7l-.235.726zm-12 4l.618.449-.236.725.617-.448.618.448-.236-.725.618-.449h-.764L3 11l-.235.726zM6.383 12.9L7 12.452l.618.448-.236-.725.618-.449h-.764L7 11l-.235.726h-.764l.618.449zm3.618-1.174l.618.449-.236.725.617-.448.618.448-.236-.725.618-.449h-.764L11 11l-.235.726zm4 0l.618.449-.236.725.617-.448.618.448-.236-.725.618-.449h-.764L15 11l-.235.726zm-12 4l.618.449-.236.725.617-.448.618.448-.236-.725.618-.449h-.764L3 15l-.235.726zM6.383 16.9L7 16.452l.618.448-.236-.725.618-.449h-.764L7 15l-.235.726h-.764l.618.449zm3.618-1.174l.618.449-.236.725.617-.448.618.448-.236-.725.618-.449h-.764L11 15l-.235.726zm4 0l.618.449-.236.725.617-.448.618.448-.236-.725.618-.449h-.764L15 15l-.235.726z'/%3E%3C/svg%3E");
  }

  .flag-ca {
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 36 36'%3E%3Cpath fill='%23D52B1E' d='M4 5C1.791 5 0 6.791 0 9v18c0 2.209 1.791 4 4 4h6V5H4zm28 0h-6v26h6c2.209 0 4-1.791 4-4V9c0-2.209-1.791-4-4-4z'/%3E%3Cpath fill='%23EEE' d='M10 5h16v26H10z'/%3E%3Cpath fill='%23D52B1E' d='M18.615 22.113c1.198.139 2.272.264 3.469.401l-.305-1.002c-.049-.176.021-.368.159-.476l3.479-2.834-.72-.339c-.317-.113-.23-.292-.115-.722l.531-1.936-2.021.427c-.197.03-.328-.095-.358-.215l-.261-.911-1.598 1.794c-.227.288-.687.288-.544-.376l.683-3.634-.917.475c-.257.144-.514.168-.657-.089l-1.265-2.366v.059-.059l-1.265 2.366c-.144.257-.401.233-.658.089l-.916-.475.683 3.634c.144.664-.317.664-.544.376l-1.598-1.793-.26.911c-.03.12-.162.245-.359.215l-2.021-.427.531 1.936c.113.43.201.609-.116.722l-.72.339 3.479 2.834c.138.107.208.3.158.476l-.305 1.002 3.47-.401c.106 0 .176.059.175.181l-.214 3.704h.956l-.213-3.704c.002-.123.071-.182.177-.182z'/%3E%3C/svg%3E");
  }
`;
