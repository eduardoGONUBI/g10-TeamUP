import React from 'react';

interface ChatCardProps {
  id: number;
  name: string;
  sport: string;
  onClick: (id: number) => void;
}

const ChatCard: React.FC<ChatCardProps> = ({ id, name, sport, onClick }) => (
  <div className="chat-card" onClick={() => onClick(id)}>
    <div className="chat-icon">🏷️</div>
    <div className="chat-info">
      <h3>{name}</h3>
      <p>{sport}</p>
    </div>
    <button className="see-chat">See Chat &gt;</button>
  </div>
);

export default ChatCard;
