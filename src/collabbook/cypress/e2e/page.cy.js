describe('Page Layout and Navigation E2E Tests', () => {

  // Run before each test to establish baseline state
  beforeEach(() => {
    // Intercept API calls to prevent polluting real database records
    cy.intercept('GET', '/api/pages/1', {
      statusCode: 200,
      body: {
        firstWord: {
          id: 101,
          content: 'Hello',
          nextWord: { id: 102, content: 'Cypress', nextWord: null }
        },
        lastWordIdOfPreviousPage: null
      }
    }).as('getPageData');
  });

  it('should render the page structure and fetch words successfully', () => {
    cy.visit('/pages/1');
    cy.wait('@getPageData');

    // Assert structural visibility elements are met
    cy.get('nav').should('be.visible');
    cy.get('.sentence-container').should('be.visible');

    // Check if words streamed onto the layout screen properly
    cy.get('.sentence-container').should('contain', 'Hello');
    cy.get('.sentence-container').should('contain', 'Cypress');
  });

  it('should redirect unauthenticated users to login when trying to edit', () => {
    cy.visit('/pages/1');
    cy.wait('@getPageData');

    // Click the Edit button while logged out
    cy.get('.mode-toggle button').contains('Edit Page').click();

    // Verify redirection criteria query signatures match
    cy.url().should('include', '/login');
    cy.url().should('include', 'redirectFrom=/pages/1/edit');
  });

  it('should allow a newly signed up user to enter edit mode via redirect', () => {
    // 1. Intercept your exact backend auth endpoints
    cy.intercept('POST', '**/auth/signup', {
      statusCode: 201,
      body: { success: true }
    }).as('signupRequest');

    // A valid structurally-formed JWT that decodes to: { exp: 4102444800 } (Year 2100)
    const validMockJwt = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjQxMDI0NDQ4MDB9.bW9ja2VkLXNpZ25hdHVyZQ';

    cy.intercept('POST', '**/auth/login', {
      statusCode: 200,
      body: {
        token: validMockJwt,
        username: 'e2e_author'
      }
    }).as('loginRequest');

    // Intercept the data fetch that happens immediately upon landing on the edit page
    cy.intercept('GET', '**/api/pages/1', {
      statusCode: 200,
      body: {
        firstWord: { id: 101, content: 'Hello', nextWord: null }
      }
    }).as('getEditPageData');

    // 2. Navigate to signup with the redirect query parameter
    cy.visit('/signup?redirectFrom=/pages/1/edit');

    // 3. Fill out the signup form
    cy.get('#username').type('e2e_author');
    cy.get('#password').type('SuperSecret123!');

    // 4. Submit the form
    cy.get('form').submit();

    // 5. Wait for both network requests to complete sequentially
    cy.wait('@signupRequest');
    cy.wait('@loginRequest');

    // 6. Assert that the router actually landed on the edit page, NOT the login query string
    cy.url().should('not.include', '/login'); // explicitly rule out the login page trap
    cy.url().should('match', /\/pages\/1\/edit$/); // matches exactly when the URL ends with your path
    cy.wait('@getEditPageData');

    // 7. Verify the edit mode controls now paint on the screen
    cy.get('.mode-toggle a').contains('View Page').should('be.visible');
    cy.get('.plus-sign').should('exist');
  });

  it('should handle real-time database error warnings gracefully', () => {
    // Intercept error layouts to watch handling boundaries
    cy.intercept('GET', '/api/pages/2', {
      statusCode: 500,
      body: { error: 'Internal Server Failure Error' }
    }).as('getBadPage');

    cy.visit('/pages/2');
    cy.wait('@getBadPage');

    // The system should cleanly draw the danger banner to screen
    cy.get('.error-alert')
      .should('be.visible')
      .and('contain', 'Error:');
  });
});

describe('Page Layout E2E Tests with Fixtures', () => {

  it('should render words cleanly from a success fixture', () => {
    // 1. Intercept the network request and provide the success fixture
    cy.intercept('GET', '/api/pages/1', { fixture: 'page-success.json' }).as('getPage');

    // 2. Visit the page and wait for the intercepted network request to finish
    cy.visit('/pages/1');
    cy.wait('@getPage');

    // 3. Make your UI assertions
    cy.get('.sentence-container')
      .should('contain', 'Hello')
      .and('contain', 'Cypress Fixtures');
  });

  it('should display a danger banner when the server returns an error fixture', () => {
    // 1. Intercept with an error status code and the error fixture payload
    cy.intercept('GET', '/api/pages/1', {
      statusCode: 500,
      fixture: 'page-error.json'
    }).as('getBadPage');

    cy.visit('/pages/1');
    cy.wait('@getBadPage');

    // 2. Assert the error UI handler caught it cleanly
    cy.get('.error-alert')
      .should('be.visible')
      .and('contain', 'Error: Database Connection Failed');
  });
});
