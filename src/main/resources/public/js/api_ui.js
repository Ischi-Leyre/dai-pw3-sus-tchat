const API_BASE = '';

async function fetchJSON(url, opts = {}) {
    try {
        const res = await fetch(API_BASE + url, opts);
        const text = await res.text();
        try { return JSON.parse(text); } catch { return { status: res.status, body: text }; }
    } catch (err) {
        return { error: true, message: err.message };
    }
}

async function getUsers(outputId) {
    const data = await fetchJSON('users');
    document.getElementById(outputId).textContent = JSON.stringify(data, null, 2);
}

async function createUser(data, outputId) {
    const res = await fetch(API_BASE+'users', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(data)});
    const json = await res.json();
    document.getElementById(outputId).textContent = JSON.stringify(json, null,2);
}

async function deleteUser(userId, outputId) {
    const res = await fetch(API_BASE+'users/'+userId, {method:'DELETE'});
    document.getElementById(outputId).textContent = `Status: ${res.status}`;
}

async function getProfile(outputId){
    const data = await fetchJSON('profile');
    document.getElementById(outputId).textContent = JSON.stringify(data,null,2);
}

async function editUser(outputId, data){
    const profile = await fetchJSON('profile');
    if(!profile.userId){ document.getElementById(outputId).textContent="Error: not logged"; return; }
    const res = await fetch(API_BASE+'users/'+profile.userId,{method:'PATCH', headers:{'Content-Type':'application/json'}, body:JSON.stringify(data)});
    const json = await res.json();
    document.getElementById(outputId).textContent=JSON.stringify(json,null,2);
}

async function getAllMessages(outputId){
    const data = await fetchJSON('messages');
    document.getElementById(outputId).textContent = JSON.stringify(data,null,2);
}

async function getMyMessages(outputId){
    const data = await fetchJSON('messages/mine');
    document.getElementById(outputId).textContent = JSON.stringify(data,null,2);
}

async function postMessage(data, outputId){
    const res = await fetch(API_BASE+'messages',{method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(data)});
    const json = await res.json();
    document.getElementById(outputId).textContent = JSON.stringify(json,null,2);
}