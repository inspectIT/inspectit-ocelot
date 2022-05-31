import { render, screen, within } from '@testing-library/react';
import LoginView from '../LoginView';
import '@testing-library/jest-dom';
import React from 'react';
import { storeWrapper } from '../../../lib/reduxTestUtils';
import { authentication } from '../../../redux/ducks';
import userEvent from '@testing-library/user-event';

const setup = () => {
  const reducers = { authentication };
  return render(storeWrapper(<LoginView />, reducers));
};

describe('LoginView', () => {
  //Arrange
  beforeEach(() => setup());

  it('renders successfully', () => {
    //Arrange
    const logo = screen.getByRole('img');
    const heading1 = screen.getByText('inspectIT Ocelot');
    const heading2 = screen.getByText('Configuration Server');
    const username = screen.getByRole('textbox', { placeholder: 'Username' });
    const password = screen.getByRole('textbox', { placeholder: 'Password' });
    const loginButton = screen.getByRole('button', { name: 'Login' });
    const footer = screen.getByText(/inspectit ocelot configuration server/i);
    const docsLink = within(footer).getByRole('link', { name: 'Docs' });
    const githubLink = within(footer).getByRole('link', { name: 'Github' });

    //Act - not required

    //Assert
    expect(logo).toBeInTheDocument();
    expect(heading1).toBeInTheDocument();
    expect(heading2).toBeInTheDocument();
    expect(username).toBeInTheDocument();
    expect(password).toBeInTheDocument();
    expect(loginButton).toBeInTheDocument();
    expect(docsLink).toBeInTheDocument();
    expect(githubLink).toBeInTheDocument();
  });

  it('disables the login button when username is missing', async () => {
    //Arrange
    const loginButton = screen.getByRole('button', { name: 'Login' });
    const password = screen.getByPlaceholderText('Password');

    //Act
    await userEvent.type(password, 'password123');

    //Assert
    expect(loginButton).toBeDisabled();
  });

  it('disables the login button when password is missing', async () => {
    //Arrange
    const loginButton = screen.getByRole('button', { name: 'Login' });
    const username = screen.getByPlaceholderText('Username');

    //Act
    await userEvent.type(username, 'userName');

    //Assert
    expect(loginButton).toBeDisabled();
  });

  it('enables the login button when username and password are present', async () => {
    //Arrange
    const loginButton = screen.getByRole('button', { name: 'Login' });
    const username = screen.getByPlaceholderText('Username');
    const password = screen.getByPlaceholderText('Password');

    //Act
    await userEvent.type(username, 'userName');
    await userEvent.type(password, 'password123');

    //Assert
    expect(loginButton).toBeEnabled();
  });
});
