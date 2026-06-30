const API_URL = "http://localhost:8080/api/ranking";

async function loadRanking() {
  const statusEl = document.getElementById("status");
  const bodyEl = document.getElementById("ranking-body");

  try {
    const response = await fetch(API_URL);

    if (!response.ok) {
      throw new Error(`Erreur API : ${response.status}`);
    }

    const players = await response.json();

    if (players.length === 0) {
      statusEl.textContent = "Aucun joueur trouvé pour le moment.";
      return;
    }

    bodyEl.innerHTML = players.map((player, index) => `
      <tr>
        <td>${index + 1}</td>
        <td>${player.playerName}</td>
        <td>${player.totalPoints}</td>
        <td>${player.tournamentsPlayed}</td>
        <td>${player.bestPlacement}</td>
      </tr>
    `).join("");

    statusEl.textContent = `Classement basé sur ${players.length} joueurs.`;

  } catch (error) {
    statusEl.textContent = "Impossible de charger le classement";
    console.error(error);
  }
}

loadRanking();
