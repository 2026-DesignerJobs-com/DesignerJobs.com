/* DESIGNJOBS.COM — register.js
   Role toggle (CLIENT/DESIGNER) + register form submit to
   POST /auth/register, logging the user in on success. Requires
   auth.js (Auth.*) and API_BASE from common.js. */
const designerRadio = document.getElementById('role-designer');
const clientRadio = document.getElementById('role-client');
const designerFields = document.getElementById('designer-fields');
const roleInput = document.getElementById('role-input');


function syncRole() {
  const role = clientRadio.checked ? 'CLIENT' : 'DESIGNER';
  roleInput.value = role;
  designerFields.style.display = role === 'DESIGNER' ? '' : 'none';
}

designerRadio.addEventListener('change', syncRole);
clientRadio.addEventListener('change', syncRole);
syncRole();

/*
 * Connects the register form with the Spring backend.
 * The form data is sent as JSON to POST {API_BASE}/auth/register.
 */
document.getElementById('register-form').addEventListener('submit', async event => {
  event.preventDefault();

  const form = event.target;
  const formData = new FormData(form);
  const message = document.getElementById('register-message');

  const registerData = {
    fullName: formData.get('name'),
    email: formData.get('email'),
    password: formData.get('password'),
    role: formData.get('role'),
    designType: formData.get('designType'),
    skills: formData.get('skills')
  };


  try {
    const response = await fetch(`${API_BASE}/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(registerData)
    });

    const text = await response.text();
    const data = text ? JSON.parse(text) : {};

    if (!response.ok) {
      message.textContent = data && data.error ? data.error : 'Registration failed.';
      message.className = 'font-monospace small mb-3 text-danger';
      return;
    }

    // Backend returns {token, userId, role} on register — log the user in immediately.
    if (window.Auth) {
      Auth.saveSession({ token: data.token, userId: data.userId, role: data.role });
    }

    message.textContent = 'Account created. Redirecting…';
    message.className = 'font-monospace small mb-3 text-success';
    window.location.href = 'homepage.html';

  } catch (error) {
    console.error('Register error:', error);
    message.textContent = 'Register request failed. Please check if the backend is running.';
    message.className = 'font-monospace small mb-3 text-danger';
  }
});
