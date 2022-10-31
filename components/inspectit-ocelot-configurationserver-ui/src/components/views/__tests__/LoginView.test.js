import { render, screen } from '@testing-library/react';
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
  let container;
  //Arrange
  beforeEach(() => {
    const renderedDom = setup();
    container = renderedDom.container;
  });

  it('renders successfully', () => {
    expect(container).toMatchSnapshot();
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
